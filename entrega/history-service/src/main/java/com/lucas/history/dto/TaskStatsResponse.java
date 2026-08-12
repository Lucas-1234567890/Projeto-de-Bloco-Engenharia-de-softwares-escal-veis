package com.lucas.history.dto;

import java.time.LocalDateTime;

/**
 * Métricas derivadas do histórico de uma task — o tipo de coisa que
 * justifica ter uma tabela de eventos própria em vez de só um
 * "updated_at": tempo até conclusão, quantas vezes foi reaberta, etc.
 */
public class TaskStatsResponse {

    private Long taskId;
    private int totalEventos;
    private LocalDateTime criadoEm;
    private LocalDateTime concluidoEm;
    private Long tempoAteConclusaoSegundos;
    private int quantidadeConclusoes;
    private int reaberturas;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public int getTotalEventos() { return totalEventos; }
    public void setTotalEventos(int totalEventos) { this.totalEventos = totalEventos; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }

    public Long getTempoAteConclusaoSegundos() { return tempoAteConclusaoSegundos; }
    public void setTempoAteConclusaoSegundos(Long tempoAteConclusaoSegundos) { this.tempoAteConclusaoSegundos = tempoAteConclusaoSegundos; }

    public int getQuantidadeConclusoes() { return quantidadeConclusoes; }
    public void setQuantidadeConclusoes(int quantidadeConclusoes) { this.quantidadeConclusoes = quantidadeConclusoes; }

    public int getReaberturas() { return reaberturas; }
    public void setReaberturas(int reaberturas) { this.reaberturas = reaberturas; }
}
