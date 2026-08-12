# Gerenciador de Tarefas

Aplicação full stack em **arquitetura de microsserviços**, com **Spring Boot** no back-end, **React + Vite** no front-end e **Eureka** para descoberta de serviços.

O projeto nasceu como um monólito em camadas (CRUD de tarefas + histórico de auditoria) e evoluiu, na Terceira Entrega, para uma arquitetura distribuída: o histórico de alterações foi extraído do `todo-api` para um microsserviço dedicado, o `history-service`, com banco de dados próprio e comunicação via REST/Feign, registrada dinamicamente no Eureka.

## Funcionalidades

* Criar tarefas
* Listar tarefas com paginação
* Filtrar por status (pendentes/concluídas)
* Buscar tarefas por título
* Atualizar tarefas
* Marcar tarefas como concluídas
* Excluir tarefas
* Consultar histórico completo de alterações de cada tarefa (via `history-service`)
* Consultar estatísticas derivadas do histórico: tempo até a primeira conclusão e quantidade de reaberturas (via `history-service`)

---

# Arquitetura

```
                         ┌──────────────────────┐
                         │   discovery-service   │
                         │   (Eureka Server)     │
                         │      :8761            │
                         └──────────▲────────────┘
                                    │ registro / descoberta
                    ┌───────────────┴───────────────┐
                    │                                │
          ┌─────────▼─────────┐            ┌─────────▼─────────┐
 React ──►│      todo-api      │──Feign───► │   history-service  │
  :5173   │       :8080        │  (REST)    │        :8082       │
          └─────────┬──────────┘            └─────────┬──────────┘
                    │                                  │
              ┌─────▼─────┐                      ┌─────▼──────┐
              │  Postgres  │                      │  Postgres  │
              │  tododb    │                      │  historydb │
              │   :5432    │                      │   :5433    │
              └────────────┘                      └────────────┘
```

* O **front-end** fala apenas com o `todo-api`, que continua sendo o único host conhecido pela interface.
* O `todo-api` descobre o endereço do `history-service` **em tempo de execução**, perguntando ao Eureka — nenhuma URL é fixada em código (`@FeignClient(name = "history-service")`).
* Cada serviço tem **seu próprio banco de dados** (padrão *database-per-service*): o `todo-api` não acessa a tabela de histórico diretamente, e o `history-service` não sabe nada sobre a tabela `tasks`.
* Falhas de comunicação com o `history-service` **não derrubam o CRUD principal** de tarefas: o registro de histórico é feito de forma resiliente (try/catch em torno da chamada Feign), já que auditoria é uma funcionalidade auxiliar.

---

# Tecnologias

| Camada             | Tecnologia                              |
| ------------------ | ---------------------------------------- |
| Back-end           | Spring Boot 3.2                          |
| Linguagem          | Java 17                                  |
| Service Discovery  | Spring Cloud Netflix Eureka              |
| Comunicação entre serviços | Spring Cloud OpenFeign            |
| Banco de produção  | PostgreSQL 16 (uma instância por serviço) |
| Banco de testes    | H2 (em memória)                          |
| Persistência       | Spring Data JPA + Hibernate              |
| Migrações          | Flyway                                   |
| Front-end          | React 18 + Vite                          |
| Build               | Maven                                    |
| Containers          | Docker + Docker Compose                  |

---

# Estrutura do Projeto

```
entrega/
├── discovery-service/     # Eureka Server — registro e descoberta dos demais serviços
│   ├── src
│   └── pom.xml
│
├── history-service/        # Microsserviço de histórico/auditoria (banco próprio)
│   ├── src
│   └── pom.xml
│
├── todo-api/                # API principal de tarefas, cliente Feign do history-service
│   ├── src
│   └── pom.xml
│
├── todo-frontend/           # Interface React (fala só com o todo-api)
│   ├── src
│   └── package.json
│
└── docker-compose.yml       # Sobe os dois bancos Postgres (tododb e historydb)

README.md
```

---

# Como executar

## Pré-requisitos

* Java 17+
* Maven 3.8+
* Node.js 18+
* Docker

## Ordem de inicialização

Os serviços têm dependência de registro no Eureka, então a ordem importa: o `discovery-service` precisa estar de pé antes dos demais subirem, para que `todo-api` e `history-service` consigam se registrar.

### 1. Bancos de dados

```bash
cd entrega
docker compose up -d
```

Sobe dois containers Postgres isolados: `tododb` (porta `5432`, usado pelo `todo-api`) e `historydb` (porta `5433`, usado pelo `history-service`).

### 2. Eureka (discovery-service)

```bash
cd discovery-service
mvn spring-boot:run
```

Aguarde subir em `http://localhost:8761` — dá pra acompanhar os serviços se registrando pelo dashboard do Eureka no navegador.

### 3. history-service

```bash
cd history-service
mvn spring-boot:run
```

Sobe em `http://localhost:8082` e se registra no Eureka como `history-service`.

### 4. todo-api

```bash
cd todo-api
mvn spring-boot:run
```

Sobe em `http://localhost:8080`, se registra como `todo-api` e descobre o `history-service` via Eureka (sem URL fixa no código).

### 5. Front-end

```bash
cd todo-frontend
npm install
npm run dev
```

Interface disponível em `http://localhost:5173`.

As migrations do Flyway de cada serviço são executadas automaticamente na inicialização.

---

# Executando os testes

```bash
cd todo-api && mvn test
cd history-service && mvn test
```

Ambos os módulos usam banco **H2 em memória** nos testes, isolando totalmente das instâncias PostgreSQL de produção.

---

# Modelagem do Domínio

O modelo de domínio foi atualizado para refletir a extração do microsserviço: a tabela `task_history`, que antes vivia no mesmo banco do `todo-api`, passou a pertencer ao `history-service`, em um banco de dados próprio.

```
todo-api (tododb)                    history-service (historydb)
──────────────────                   ────────────────────────────
tasks                                task_history
├── id (PK)                          ├── id (PK)
├── titulo                           ├── task_id      (sem FK física)
├── descricao                        ├── action        CREATED | UPDATED | COMPLETED | DELETED
├── completed                        ├── titulo_snapshot
├── created_at                       ├── descricao_snapshot
└── updated_at                       ├── completed_snapshot
                                      └── changed_at
```

O `todo-api` não mantém mais nenhuma tabela de histórico local: a migration `V3__drop_task_history_table.sql` remove a tabela antiga do monólito, deixando explícito no versionamento que a responsabilidade migrou de serviço.

## Por que `task_history.task_id` não possui Foreign Key?

O histórico precisa sobreviver mesmo depois que a task original é excluída no `todo-api` — e, num cenário de microsserviços, isso fica ainda mais evidente: `task_id` referencia uma linha que mora em **outro banco de dados**, então uma FK física nem seria tecnicamente possível. O relacionamento é tratado inteiramente pela aplicação.

## Por que extrair o histórico para um microsserviço em vez de manter no monólito?

* Isola a responsabilidade de auditoria, que tem um ciclo de vida e um volume de dados diferentes do CRUD de tarefas.
* Permite evoluir e escalar o histórico (relatórios, métricas de produtividade) de forma independente do `todo-api`.
* Torna a comunicação explícita via contrato REST (DTOs próprios em cada lado, sem biblioteca compartilhada), evitando acoplamento por código.
* Demonstra na prática o padrão *database-per-service*: nenhum serviço acessa a tabela do outro diretamente.

---

# Comunicação entre Serviços

A comunicação `todo-api → history-service` é feita via **Feign Client** declarativo, com o endereço resolvido dinamicamente pelo Eureka:

```java
@FeignClient(name = "history-service")
public interface HistoryClient {

    @PostMapping("/api/history")
    TaskHistoryResponse registrarEvento(@RequestBody TaskHistoryRequest request);

    @GetMapping("/api/history/task/{taskId}")
    List<TaskHistoryResponse> buscarHistorico(@PathVariable("taskId") Long taskId);

    @GetMapping("/api/history/task/{taskId}/estatisticas")
    TaskStatsResponse buscarEstatisticas(@PathVariable("taskId") Long taskId);
}
```

O `name` do `@FeignClient` é o mesmo `spring.application.name` configurado no `history-service` — não há host nem porta fixados no código do `todo-api`.

**Resiliência:** o registro de eventos (`registrarHistorico`) é envolto em `try/catch` para `FeignException`. Se o `history-service` estiver indisponível, a operação de CRUD na task segue normalmente e apenas um warning é logado — o histórico não é uma dependência crítica da operação principal.

---

# API REST

## todo-api (`http://localhost:8080`) — ponto único de acesso do front-end

| Método | Endpoint                     | Descrição                                    |
| ------ | ----------------------------- | ---------------------------------------------- |
| GET    | `/api/tasks`                  | Listar tarefas                                 |
| GET    | `/api/tasks?page=0&size=20`   | Paginação                                      |
| GET    | `/api/tasks?completed=true`   | Filtrar por status                             |
| GET    | `/api/tasks?titulo=texto`     | Buscar por título                              |
| GET    | `/api/tasks/{id}`             | Buscar tarefa por ID                           |
| POST   | `/api/tasks`                  | Criar tarefa                                   |
| PUT    | `/api/tasks/{id}`             | Atualizar tarefa                               |
| PATCH  | `/api/tasks/{id}/concluir`    | Concluir tarefa                                |
| DELETE | `/api/tasks/{id}`             | Excluir tarefa                                 |
| GET    | `/api/tasks/{id}/historico`   | Histórico da tarefa *(proxy para history-service)* |
| GET    | `/api/tasks/{id}/estatisticas`| Estatísticas da tarefa *(proxy para history-service)* |

## history-service (`http://localhost:8082`) — consumido internamente pelo todo-api

| Método | Endpoint                            | Descrição                                    |
| ------ | ------------------------------------ | ----------------------------------------------- |
| POST   | `/api/history`                       | Registrar um evento de auditoria (CREATED, UPDATED, COMPLETED, DELETED) |
| GET    | `/api/history/task/{taskId}`         | Listar eventos de uma tarefa, mais recentes primeiro |
| GET    | `/api/history/task/{taskId}/estatisticas` | Métricas derivadas: total de eventos, data de criação/conclusão, tempo até a 1ª conclusão e número de reaberturas |

O front-end nunca chama o `history-service` diretamente — sempre passa pelos endpoints `/api/tasks/{id}/historico` e `/api/tasks/{id}/estatisticas` do `todo-api`.

---

# Camada de Persistência

## todo-api

* **Task** — entidade principal, com `@Entity`, `@Table`, Bean Validation (`@NotBlank`, `@Size`) e auditoria automática (`@CreatedDate`, `@LastModifiedDate`, `@EnableJpaAuditing`).
* **TaskRepository** — Spring Data JPA, com consultas por status e por título:

```java
Page<Task> tarefasPendentes =
    taskRepository.findByCompleted(false, PageRequest.of(0, 20));

Page<Task> resultado =
    taskRepository.findByTituloContainingIgnoreCase("relatório", pageable);
```

## history-service

* **TaskHistory** — snapshot imutável do estado da task no momento do evento, criado explicitamente pelo `TaskHistoryService` a cada chamada recebida via Feign.
* **TaskHistoryRepository** — consultas por task, ordenadas por data:

```java
List<TaskHistory> eventos =
    taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId);
```

---

# Tratamento de Erros

Ambos os serviços usam `@RestControllerAdvice` para padronizar respostas de erro:

* **400 Bad Request** → dados inválidos
* **404 Not Found** → tarefa ou histórico inexistente

O `todo-api` também traduz `FeignException.NotFound` vindo do `history-service` em `TaskNotFoundException`, mantendo o contrato de erro consistente para o front-end mesmo quando o dado vem de outro serviço.

---

# Testes

| Serviço          | Camada     | Tipo             | Ferramenta               |
| ----------------- | ---------- | ---------------- | -------------------------- |
| todo-api           | Repository | Integração        | `@DataJpaTest`              |
| todo-api           | Service    | Unitário          | JUnit 5 + Mockito           |
| todo-api           | Controller | Integração HTTP   | `@WebMvcTest` + MockMvc     |
| history-service     | Repository | Integração        | `@DataJpaTest`              |
| history-service     | Service    | Unitário          | JUnit 5 + Mockito           |
| history-service     | Controller | Integração HTTP   | `@WebMvcTest` + MockMvc     |

Cobertura inclui: criação, atualização, conclusão e exclusão de tarefas; registro e consulta de histórico; cálculo de estatísticas (tempo até conclusão, reaberturas); paginação; filtros; validações; e tratamento de erros — no `todo-api` e, agora, também no `history-service` isoladamente.

---

# Front-End

O componente `HistoryModal` consome os endpoints de histórico e estatísticas do `todo-api`, exibindo:

* linha do tempo dos eventos da tarefa (criada, atualizada, concluída, excluída);
* painel de métricas: total de eventos, quantidade de conclusões, reaberturas e tempo até a primeira conclusão.

O front-end continua conhecendo apenas `http://localhost:8080` — a existência do `history-service` é um detalhe de infraestrutura invisível para a interface.

---

# Possíveis Evoluções

* Autenticação com Spring Security + JWT
* Controle de usuários e permissões
* Gateway único (Spring Cloud Gateway) na frente de `todo-api` e `history-service`
* Circuit breaker (Resilience4j) nas chamadas Feign
* Dockerização completa da aplicação (incluindo os serviços Java e o front-end)
* Pipeline CI/CD com GitHub Actions
* Deploy em nuvem (Render, Railway ou AWS)
* Documentação da API com Swagger/OpenAPI
* Dashboard com métricas de produtividade a partir do `history-service`
