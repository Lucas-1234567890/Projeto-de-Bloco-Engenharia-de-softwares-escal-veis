-- Tabela de auditoria: registra todo evento relevante ocorrido em uma task.
-- Migrada do monólito todo-api (era V2 lá) para este microsserviço dedicado.
-- Desenhada para consulta por task (rastreabilidade) e por período (auditoria).
CREATE TABLE task_history (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL,
    action      VARCHAR(20) NOT NULL,   -- CREATED | UPDATED | COMPLETED | DELETED
    titulo_snapshot     VARCHAR(200),
    descricao_snapshot  VARCHAR(1000),
    completed_snapshot  BOOLEAN,
    changed_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Task_id não é FK física de propósito: o histórico precisa sobreviver
-- mesmo depois que a task original é deletada no todo-api, e nem faria
-- sentido ter FK apontando para uma tabela que agora mora em outro banco.
CREATE INDEX idx_task_history_task_id ON task_history (task_id);
CREATE INDEX idx_task_history_changed_at ON task_history (changed_at);
