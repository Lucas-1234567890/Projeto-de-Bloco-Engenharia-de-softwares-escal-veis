# Gerenciador de Tarefas

Aplicação full stack em **arquitetura de microsserviços orientada a eventos**, com **Spring Boot** no back-end, **React + Vite** no front-end, **Eureka** para descoberta de serviços e **RabbitMQ** como message broker.

O projeto nasceu como um monólito em camadas (CRUD de tarefas + histórico de auditoria) e evoluiu, na Terceira Entrega, para uma arquitetura distribuída: o histórico de alterações foi extraído do `todo-api` para um microsserviço dedicado, o `history-service`, com banco de dados próprio e comunicação via REST/Feign, registrada dinamicamente no Eureka.

Na **Quarta Entrega**, a escrita do histórico foi refatorada de chamada REST síncrona para **eventos assíncronos via RabbitMQ**: o `todo-api` publica um evento a cada mudança de task e os interessados (`history-service`, `notification-service`) o consomem de forma independente. A documentação completa da refatoração — prós e contras, padrões de mensagens, diagramas e roteiro de demonstração — está em **[docs/tp4-arquitetura-orientada-a-eventos.md](docs/tp4-arquitetura-orientada-a-eventos.md)**.

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
* Notificação quando uma tarefa é concluída (via `notification-service`, reagindo ao evento `task.completed`)

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
            ┌─────────▼─────────┐  consulta   ┌─────────▼─────────┐
 React ────►│      todo-api      │───Feign────►│   history-service  │
  :5173     │       :8080        │   (REST)    │        :8082       │
            └───┬────────┬───────┘             └──▲────────┬────────┘
                │        │                        │        │
                │        │ publica eventos        │consome │
                │        ▼                        │        │
                │  ┌───────────────────────────┐  │        │
                │  │   RabbitMQ  :5672          │──┘        │
                │  │   exchange "task.events"   │           │
                │  │   (topic)                  │──┐        │
                │  └───────────────────────────┘  │        │
                │                                  ▼        │
          ┌─────▼─────┐             ┌──────────────────┐  ┌──▼─────────┐
          │  Postgres  │             │ notification-     │  │  Postgres  │
          │  tododb    │             │ service  :8083    │  │  historydb │
          │   :5432    │             └──────────────────┘  │   :5433    │
          └────────────┘                                    └────────────┘
```

* O **front-end** fala apenas com o `todo-api`, que continua sendo o único host conhecido pela interface.
* **Escritas viram eventos (assíncrono)**: a cada criação/atualização/conclusão/exclusão o `todo-api` publica um evento no exchange `task.events` do RabbitMQ — **depois do commit** da transação. Quem precisa reagir declara uma fila própria e a liga ao exchange; o produtor não conhece os consumidores.
* **Leituras continuam síncronas**: o histórico e as estatísticas são consultados via Feign, com o endereço do `history-service` descoberto pelo Eureka (`@FeignClient(name = "history-service")`).
* Cada serviço tem **seu próprio banco de dados** (padrão *database-per-service*): o `todo-api` não acessa a tabela de histórico diretamente, e o `history-service` não sabe nada sobre a tabela `tasks`.
* O CRUD **não depende mais da disponibilidade do `history-service`** (nem do broker): se o consumidor estiver fora do ar, as mensagens esperam na fila durável e são processadas quando ele voltar.
* Mensagens que o consumidor não consegue processar passam por retry com backoff e, esgotadas as tentativas, vão para uma **Dead Letter Queue**; o consumidor é **idempotente** (`event_id` único) para tolerar entrega duplicada.

---

# Tecnologias

| Camada             | Tecnologia                              |
| ------------------ | ---------------------------------------- |
| Back-end           | Spring Boot 3.2                          |
| Linguagem          | Java 17                                  |
| Service Discovery  | Spring Cloud Netflix Eureka              |
| Comunicação síncrona (consultas) | Spring Cloud OpenFeign      |
| Comunicação assíncrona (eventos) | RabbitMQ 3.13 + Spring AMQP (`spring-boot-starter-amqp`) |
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
├── history-service/        # Microsserviço de histórico/auditoria (banco próprio); consome eventos
│   ├── src
│   └── pom.xml
│
├── notification-service/   # Reage a task.completed (demonstra fan-out; sem banco)
│   ├── src
│   └── pom.xml
│
├── todo-api/                # API principal de tarefas; produtor de eventos e cliente Feign (consultas)
│   ├── src
│   └── pom.xml
│
├── todo-frontend/           # Interface React (fala só com o todo-api)
│   ├── src
│   └── package.json
│
└── docker-compose.yml       # Sobe os dois bancos Postgres (tododb e historydb) e o RabbitMQ

docs/
└── tp4-arquitetura-orientada-a-eventos.md   # Documentação da Quarta Entrega

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

### 1. Bancos de dados e RabbitMQ

```bash
cd entrega
docker compose up -d
```

Sobe três containers: dois Postgres isolados — `tododb` (porta `5432`, usado pelo `todo-api`) e `historydb` (porta `5433`, usado pelo `history-service`) — e o **RabbitMQ** (AMQP na `5672`; painel de gerenciamento em `http://localhost:15672`, usuário/senha `todo`/`todo`).

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

Sobe em `http://localhost:8082`, se registra no Eureka como `history-service` e passa a consumir a fila `history.task-events`.

### 4. todo-api

```bash
cd todo-api
mvn spring-boot:run
```

Sobe em `http://localhost:8080`, se registra como `todo-api` e descobre o `history-service` via Eureka (sem URL fixa no código).

### 5. notification-service (opcional)

```bash
cd notification-service
mvn spring-boot:run
```

Sobe em `http://localhost:8083` e passa a consumir a fila `notification.task-completed`. Ao concluir uma tarefa, aparece uma linha `NOTIFICAÇÃO: ...` no log deste serviço. Não é necessário para o resto do sistema funcionar — esse é justamente o ponto da arquitetura orientada a eventos.

### 6. Front-end

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
cd notification-service && mvn test
```

Os módulos com banco usam **H2 em memória** nos testes, isolando totalmente das instâncias PostgreSQL de produção. Os testes **não** exigem RabbitMQ no ar: a publicação e o consumo são testados com mocks e por testes de contrato do JSON dos eventos.

---

# Modelagem do Domínio

O modelo de domínio foi atualizado para refletir a extração do microsserviço: a tabela `task_history`, que antes vivia no mesmo banco do `todo-api`, passou a pertencer ao `history-service`, em um banco de dados próprio.

```
todo-api (tododb)                    history-service (historydb)
──────────────────                   ────────────────────────────
tasks                                task_history
├── id (PK)                          ├── id (PK)
├── titulo                           ├── event_id      (UUID único: idempotência do consumidor)
├── descricao                        ├── task_id       (sem FK física)
├── completed                        ├── action        CREATED | UPDATED | COMPLETED | DELETED
├── created_at                       ├── titulo_snapshot
└── updated_at                       ├── descricao_snapshot
                                     ├── completed_snapshot
                                     └── changed_at    (= occurredAt do evento)
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

O sistema usa dois estilos de comunicação, escolhidos conforme a natureza da operação:

| Operação | Estilo | Por quê |
| -------- | ------ | ------- |
| Registrar histórico (escrita) | **Assíncrona** — evento no RabbitMQ | O `todo-api` não precisa esperar nem depender do `history-service`; múltiplos consumidores podem reagir ao mesmo fato |
| Consultar histórico/estatísticas (leitura) | **Síncrona** — Feign + Eureka | O usuário precisa da resposta na hora |

## Eventos (RabbitMQ)

Exchange `task.events` (topic, durável). Routing keys: `task.created`, `task.updated`, `task.completed`, `task.deleted`.

| Fila | Binding | Consumidor | Observação |
| ---- | ------- | ---------- | ---------- |
| `history.task-events` | `task.*` | history-service | retry (3 tentativas, backoff exponencial) → DLQ |
| `history.task-events.dlq` | via `task.events.dlx` | — (inspeção manual) | mensagens que falharam definitivamente |
| `notification.task-completed` | `task.completed` | notification-service | só conclusões |

Payload do evento (JSON):

```json
{
  "eventId": "6c1c1f0e-8a52-4a53-a1b5-0f3c8a1f7a11",
  "eventType": "COMPLETED",
  "taskId": 42,
  "titulo": "Estudar RabbitMQ",
  "descricao": "TP4",
  "completed": true,
  "occurredAt": "2026-09-20T10:30:00"
}
```

Cada serviço define **sua própria classe** para o evento (nada é compartilhado por biblioteca); o acoplamento é apenas o contrato JSON, travado por testes de contrato nos dois lados.

## Consultas (Feign)

```java
@FeignClient(name = "history-service")
public interface HistoryClient {

    @GetMapping("/api/history/task/{taskId}")
    List<TaskHistoryResponse> buscarHistorico(@PathVariable("taskId") Long taskId);

    @GetMapping("/api/history/task/{taskId}/estatisticas")
    TaskStatsResponse buscarEstatisticas(@PathVariable("taskId") Long taskId);
}
```

O `name` do `@FeignClient` é o mesmo `spring.application.name` configurado no `history-service` — não há host nem porta fixados no código do `todo-api`.

**Resiliência:** o `TaskEventPublisher` só publica após o commit da transação e, se o broker estiver indisponível, apenas registra o erro em log — o CRUD da task nunca falha por causa da mensageria. As limitações dessa abordagem (e o padrão *Transactional Outbox* como evolução) estão discutidas na [documentação do TP4](docs/tp4-arquitetura-orientada-a-eventos.md).

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

## history-service (`http://localhost:8082`) — consultas consumidas internamente pelo todo-api

| Método | Endpoint                                  | Descrição                                                                                                         |
| ------ | ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| GET    | `/api/history/task/{taskId}`              | Listar eventos de uma tarefa, mais recentes primeiro                                                              |
| GET    | `/api/history/task/{taskId}/estatisticas` | Métricas derivadas: total de eventos, data de criação/conclusão, tempo até a 1ª conclusão e número de reaberturas |

> **TP4:** o antigo `POST /api/history` foi removido. O histórico agora só é escrito ao consumir eventos do RabbitMQ (`history.task-events`). Como o processamento é assíncrono, o histórico é **eventualmente consistente**: há um pequeno atraso (normalmente milissegundos) entre criar a task e o evento aparecer na consulta.

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

* **TaskHistory** — snapshot imutável do estado da task no momento do evento, criado pelo `TaskHistoryService` a cada evento consumido do RabbitMQ (com `changed_at` vindo do `occurredAt` do evento).
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
| todo-api           | Mensageria | Unitário          | JUnit 5 + Mockito (publisher) |
| todo-api / history-service / notification-service | Contrato do evento | Unitário | Jackson (serialização/leitura do JSON) |
| history-service     | Consumidor | Unitário          | idempotência, tipo inválido, `occurredAt` |

Cobertura inclui: criação, atualização, conclusão e exclusão de tarefas (com o evento publicado em cada uma); publicação com routing key correta e tolerância a broker indisponível; consumo idempotente de eventos; contrato JSON dos eventos; consulta de histórico; cálculo de estatísticas (tempo até conclusão, reaberturas); paginação; filtros; validações; e tratamento de erros — no `todo-api` e, agora, também no `history-service` isoladamente.

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
* Circuit breaker (Resilience4j) nas chamadas Feign de consulta
* Padrão *Transactional Outbox* para eliminar a janela de perda de evento entre o commit e a publicação
* Testes de integração com RabbitMQ real via Testcontainers
* Publisher confirms e monitoramento/alerta sobre o tamanho da Dead Letter Queue
* Dockerização completa da aplicação (incluindo os serviços Java e o front-end)
* Pipeline CI/CD com GitHub Actions
* Deploy em nuvem (Render, Railway ou AWS)
* Documentação da API com Swagger/OpenAPI
* Dashboard com métricas de produtividade a partir do `history-service`
