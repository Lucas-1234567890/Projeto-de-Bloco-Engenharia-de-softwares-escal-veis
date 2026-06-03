# Gerenciador de Tarefas

Aplicação monolítica em camadas desenvolvida com **Spring Boot** (back-end) e **React** (front-end), como entrega do TP1 da disciplina de Desenvolvimento de Software.

## Funcionalidades

- Criar tarefas
- Listar tarefas
- Marcar tarefa como concluída
- Excluir tarefa

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Back-end | Spring Boot 3.x |
| Banco de dados | H2 (in-memory) |
| ORM | Spring Data JPA |
| Front-end | React 18 + Vite |
| Build | Maven |

## Como rodar

### Pré-requisitos
- Java 17+
- Maven 3.8+
- Node.js 18+

### Back-end
```bash
cd todo-api
mvn spring-boot:run
```
API disponível em: `http://localhost:8080`  
Console H2: `http://localhost:8080/h2-console` *(JDBC URL: `jdbc:h2:mem:tododb` / usuário: `sa` / senha: vazio)*

### Front-end
```bash
cd todo-frontend
npm install
npm run dev
```
Interface disponível em: `http://localhost:5173`

## Endpoints da API

| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/tasks` | Listar todas as tarefas |
| POST | `/api/tasks` | Criar nova tarefa |
| PATCH | `/api/tasks/{id}/concluir` | Marcar como concluída |
| DELETE | `/api/tasks/{id}` | Excluir tarefa |

## Arquitetura

```
React (Browser) → Controller → Service → Repository → H2
```

Segue o padrão de camadas com separação de responsabilidades:
- **Controller** — recebe requisições HTTP
- **Service** — regras de negócio
- **Repository** — acesso ao banco via Spring Data JPA
- **Model** — entidade `Task`
