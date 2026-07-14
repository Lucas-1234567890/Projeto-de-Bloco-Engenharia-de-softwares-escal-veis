# Gerenciador de Tarefas

Aplicação monolítica em camadas — **Spring Boot** (back-end) + **React** (front-end).
TP1: CRUD básico. **TP2: camada de persistência real (JPA + Spring Data) com histórico de mudanças e testes automatizados.**

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Back-end | Spring Boot 3.2 |
| Banco de dados (prod) | PostgreSQL 16 |
| Banco de dados (testes) | H2 em memória |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Front-end | React 18 + Vite |
| Build | Maven |

## Como rodar

### Pré-requisitos
- Java 17+, Maven 3.8+, Node.js 18+, Docker

### 1. Subir o banco
```bash
docker compose up -d
```

### 2. Back-end
```bash
cd todo-api
mvn spring-boot:run
```
API em `http://localhost:8080`. O Flyway aplica as migrations automaticamente na inicialização.

### 3. Front-end
```bash
cd todo-frontend
npm install
npm run dev
```
Interface em `http://localhost:5173`.

### Rodar os testes
```bash
cd todo-api
mvn test
```
Os testes usam H2 em memória (`src/test/resources/application.properties`) — não tocam no Postgres.

## Modelagem de dados

```
tasks                          task_history
├── id (PK)                    ├── id (PK)
├── titulo                     ├── task_id        (sem FK — ver nota abaixo)
├── descricao                  ├── action          (CREATED/UPDATED/COMPLETED/DELETED)
├── completed                  ├── titulo_snapshot
├── created_at                 ├── descricao_snapshot
└── updated_at                 ├── completed_snapshot
                                └── changed_at
```

**Por que `task_history.task_id` não é uma foreign key física:** o histórico existe justamente
para sobreviver à exclusão da task original — é o registro de auditoria de que ela existiu e do
que aconteceu com ela. Se fosse FK com `ON DELETE CASCADE`, o histórico morreria junto com a task,
o que anula o propósito da funcionalidade. Se fosse FK sem cascade, a query de `DELETE` quebraria.
Por isso o relacionamento é lógico (aplicação), não imposto pelo schema.

**Por que histórico como tabela própria e não Hibernate Envers:** Envers automatiza a auditoria,
mas esconde o "como" — difícil de customizar quando você precisa, por exemplo, cruzar eventos de
histórico com métricas de negócio (tempo médio até conclusão, taxa de reabertura de tarefas). Uma
tabela explícita com snapshot dá controle total sobre o que é gravado e como é consultado, e
evolui naturalmente para relatórios de auditoria.

## Camada de persistência

### `Task` (entidade principal)
Mapeada com JPA (`@Entity`, `@Table`), validação Bean Validation (`@NotBlank`, `@Size`) e
auditoria automática de `createdAt`/`updatedAt` via `@CreatedDate`/`@LastModifiedDate` +
`@EnableJpaAuditing`.

### `TaskHistory` (auditoria)
Snapshot imutável do estado da task em cada evento relevante. Gravado explicitamente no
`TaskService`, não via listener de entidade — mantém a lógica testável e visível, sem "mágica"
escondida em callbacks de ciclo de vida do JPA.

### Repositórios (Spring Data)
```java
// Paginação + filtro por status
Page<Task> tarefasPendentes = taskRepository.findByCompleted(false, PageRequest.of(0, 20));

// Busca por título, case-insensitive
Page<Task> resultado = taskRepository.findByTituloContainingIgnoreCase("relatório", pageable);

// Histórico completo de uma task, mais recente primeiro
List<TaskHistory> eventos = taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId);
```

## Endpoints da API

| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/tasks?page=0&size=20` | Listar com paginação |
| GET | `/api/tasks?completed=true` | Filtrar por status |
| GET | `/api/tasks?titulo=texto` | Buscar por título |
| GET | `/api/tasks/{id}` | Buscar uma tarefa |
| POST | `/api/tasks` | Criar tarefa |
| PUT | `/api/tasks/{id}` | Atualizar tarefa |
| PATCH | `/api/tasks/{id}/concluir` | Marcar como concluída |
| DELETE | `/api/tasks/{id}` | Excluir tarefa |
| GET | `/api/tasks/{id}/historico` | Histórico de eventos da tarefa |

Erros retornam JSON estruturado (404 para tarefa inexistente, 400 para validação falha) via
`@RestControllerAdvice`, em vez de 500 genérico.

## Testes automatizados

| Camada | Tipo de teste | Ferramenta |
|--------|---------------|-----------|
| `TaskRepository` | Integração com banco real (H2) | `@DataJpaTest` |
| `TaskService` | Unitário, com mocks | JUnit 5 + Mockito |
| `TaskController` | Integração HTTP simulada | `@WebMvcTest` + MockMvc |

Cobrem: paginação, filtros, geração de histórico em cada ação (criar/atualizar/concluir/deletar),
tratamento de 404 e validação de entrada.

## Arquitetura

```
React (Browser) → Controller → Service → Repository → PostgreSQL
                                   ↓
                          TaskHistoryRepository (auditoria)
```
