package com.lucas.todo.client.dto;

import java.time.LocalDateTime;

/** Espelha o JSON retornado pelo history-service para um evento de histórico. */
public class TaskHistoryResponse {

    private Long id;
    private Long taskId;
    private String action;
    private String tituloSnapshot;
    private String descricaoSnapshot;
    private Boolean completedSnapshot;
    private LocalDateTime changedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTituloSnapshot() { return tituloSnapshot; }
    public void setTituloSnapshot(String tituloSnapshot) { this.tituloSnapshot = tituloSnapshot; }

    public String getDescricaoSnapshot() { return descricaoSnapshot; }
    public void setDescricaoSnapshot(String descricaoSnapshot) { this.descricaoSnapshot = descricaoSnapshot; }

    public Boolean getCompletedSnapshot() { return completedSnapshot; }
    public void setCompletedSnapshot(Boolean completedSnapshot) { this.completedSnapshot = completedSnapshot; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
