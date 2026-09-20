-- TP4: o histórico passa a ser alimentado por eventos do RabbitMQ (entrega at-least-once).
-- event_id identifica o evento de origem e torna o consumidor idempotente:
-- se a mesma mensagem for entregue duas vezes, a segunda é descartada.
--
-- Nullable de propósito: linhas antigas (gravadas via REST antes do TP4) não têm event_id.
-- O índice único do Postgres aceita vários NULLs, então não conflita com elas.
ALTER TABLE task_history ADD COLUMN event_id UUID;

CREATE UNIQUE INDEX uq_task_history_event_id ON task_history (event_id);
