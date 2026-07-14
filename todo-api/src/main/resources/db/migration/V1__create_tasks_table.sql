CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    titulo      VARCHAR(200) NOT NULL,
    descricao   VARCHAR(1000),
    completed   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_completed ON tasks (completed);
