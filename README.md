# Gerenciador de Tarefas

Aplicação full stack desenvolvida em arquitetura monolítica em camadas utilizando **Spring Boot** no back-end e **React + Vite** no front-end.

O projeto implementa um CRUD completo de tarefas, incluindo persistência com PostgreSQL, histórico de alterações (auditoria), migrações de banco com Flyway e testes automatizados.

## Funcionalidades

* Criar tarefas
* Listar tarefas com paginação
* Filtrar por status (pendentes/concluídas)
* Buscar tarefas por título
* Atualizar tarefas
* Marcar tarefas como concluídas
* Excluir tarefas
* Consultar histórico completo de alterações de cada tarefa

---

# Tecnologias

| Camada            | Tecnologia                  |
| ----------------- | --------------------------- |
| Back-end          | Spring Boot 3.2             |
| Linguagem         | Java 17                     |
| Banco de produção | PostgreSQL 16               |
| Banco de testes   | H2 (em memória)             |
| Persistência      | Spring Data JPA + Hibernate |
| Migrações         | Flyway                      |
| Front-end         | React 18 + Vite             |
| Build             | Maven                       |
| Containers        | Docker + Docker Compose     |

---

# Estrutura do Projeto

```
todo-api/
├── src
├── pom.xml

todo-frontend/
├── src
├── package.json

docker-compose.yml
README.md
```

---

# Como executar

## Pré-requisitos

* Java 17+
* Maven 3.8+
* Node.js 18+
* Docker

---

## 1. Iniciar o banco de dados

```bash
docker compose up -d
```

---

## 2. Executar o Back-end

```bash
cd todo-api
mvn spring-boot:run
```

API disponível em:

```
http://localhost:8080
```

As migrations do Flyway são executadas automaticamente durante a inicialização da aplicação.

---

## 3. Executar o Front-end

```bash
cd todo-frontend
npm install
npm run dev
```

Interface disponível em:

```
http://localhost:5173
```

---

# Executando os testes

```bash
cd todo-api
mvn test
```

Os testes utilizam um banco **H2 em memória**, garantindo isolamento e evitando alterações no banco PostgreSQL.

---

# Modelagem do Banco

```
tasks
├── id (PK)
├── titulo
├── descricao
├── completed
├── created_at
└── updated_at

task_history
├── id (PK)
├── task_id
├── action
├── titulo_snapshot
├── descricao_snapshot
├── completed_snapshot
└── changed_at
```

---

# Decisões de Projeto

## Por que `task_history.task_id` não possui Foreign Key?

O objetivo da tabela de histórico é preservar todos os eventos da tarefa, inclusive após sua exclusão.

Caso existisse uma FK com **ON DELETE CASCADE**, o histórico seria removido junto com a tarefa.

Caso existisse uma FK sem cascade, a exclusão da tarefa seria impedida.

Por esse motivo, o relacionamento é tratado pela aplicação e não pelo banco de dados.

---

## Por que uma tabela própria de histórico?

Embora o Hibernate Envers automatize auditoria, foi escolhida uma implementação manual por oferecer maior controle sobre os dados armazenados.

Isso facilita futuras evoluções, como:

* cálculo do tempo médio até conclusão;
* métricas de produtividade;
* taxa de reabertura de tarefas;
* geração de relatórios de auditoria.

Cada evento registra um snapshot completo da tarefa no momento da alteração.

---

# Camada de Persistência

## Task

Entidade principal da aplicação.

Utiliza:

* `@Entity`
* `@Table`
* Bean Validation (`@NotBlank`, `@Size`)
* Auditoria automática com:

```
@CreatedDate
@LastModifiedDate
@EnableJpaAuditing
```

---

## TaskHistory

Representa um snapshot imutável do estado da tarefa.

Os registros são criados explicitamente na camada de serviço (`TaskService`), tornando a lógica de auditoria transparente, previsível e facilmente testável.

---

# Repositórios

Exemplos de consultas utilizando Spring Data JPA.

### Paginação

```java
Page<Task> tarefasPendentes =
    taskRepository.findByCompleted(false, PageRequest.of(0, 20));
```

### Busca por título

```java
Page<Task> resultado =
    taskRepository.findByTituloContainingIgnoreCase("relatório", pageable);
```

### Histórico da tarefa

```java
List<TaskHistory> eventos =
    taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId);
```

---

# API REST

| Método | Endpoint                    | Descrição            |
| ------ | --------------------------- | -------------------- |
| GET    | `/api/tasks`                | Listar tarefas       |
| GET    | `/api/tasks?page=0&size=20` | Paginação            |
| GET    | `/api/tasks?completed=true` | Filtrar por status   |
| GET    | `/api/tasks?titulo=texto`   | Buscar por título    |
| GET    | `/api/tasks/{id}`           | Buscar tarefa por ID |
| POST   | `/api/tasks`                | Criar tarefa         |
| PUT    | `/api/tasks/{id}`           | Atualizar tarefa     |
| PATCH  | `/api/tasks/{id}/concluir`  | Concluir tarefa      |
| DELETE | `/api/tasks/{id}`           | Excluir tarefa       |
| GET    | `/api/tasks/{id}/historico` | Histórico da tarefa  |

---

# Tratamento de Erros

A API utiliza `@RestControllerAdvice` para padronizar respostas de erro.

Exemplos:

* **400 Bad Request** → dados inválidos
* **404 Not Found** → tarefa inexistente

Todas as respostas seguem um formato JSON consistente.

---

# Testes

| Camada     | Tipo            | Ferramenta              |
| ---------- | --------------- | ----------------------- |
| Repository | Integração      | `@DataJpaTest`          |
| Service    | Unitário        | JUnit 5 + Mockito       |
| Controller | Integração HTTP | `@WebMvcTest` + MockMvc |

Os testes cobrem:

* criação de tarefas;
* atualização;
* conclusão;
* exclusão;
* geração de histórico;
* paginação;
* filtros;
* validações;
* tratamento de erros.

---

# Arquitetura

```
React
   │
   ▼
Controller
   │
   ▼
Service
   │
   ├──────────────► TaskHistoryRepository
   │                     │
   ▼                     ▼
Repository          Auditoria
   │
   ▼
PostgreSQL
```

---

# Possíveis Evoluções

* Autenticação com Spring Security + JWT
* Controle de usuários e permissões
* Dockerização completa da aplicação
* Pipeline CI/CD com GitHub Actions
* Deploy em nuvem (Render, Railway ou AWS)
* Documentação da API com Swagger/OpenAPI
* Dashboard com métricas de produtividade
