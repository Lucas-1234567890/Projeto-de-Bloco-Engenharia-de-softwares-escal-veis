-- A tabela task_history foi extraída para um microsserviço dedicado
-- (history-service), com banco de dados próprio. O todo-api deixa de ser
-- dono desses dados; a partir de agora ele fala com o history-service
-- via REST (Feign) em vez de acessar a tabela diretamente.
--
-- V1 e V2 não são reescritas de propósito: alterar migrations já aplicadas
-- quebraria o checksum do Flyway em qualquer ambiente que já tenha rodado
-- essa versão. O jeito correto de "desfazer" é uma nova migration.
DROP TABLE IF EXISTS task_history;
