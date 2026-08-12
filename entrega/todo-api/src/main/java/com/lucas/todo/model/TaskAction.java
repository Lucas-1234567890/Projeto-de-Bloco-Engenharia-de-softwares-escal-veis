package com.lucas.todo.model;

/**
 * Ações de domínio da task usadas para descrever o evento enviado ao
 * history-service. Continua existindo aqui (mesmo com um enum equivalente
 * no history-service) porque os dois serviços não compartilham código —
 * só um contrato JSON (o nome da constante).
 */
public enum TaskAction {
    CREATED,
    UPDATED,
    COMPLETED,
    DELETED
}
