package com.lucas.history.exception;

public class HistoricoNaoEncontradoException extends RuntimeException {
    public HistoricoNaoEncontradoException(Long taskId) {
        super("Nenhum histórico encontrado para taskId=" + taskId);
    }
}
