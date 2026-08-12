package com.lucas.todo.client.dto;

/** Contrato de saída enviado ao history-service ao registrar um evento. */
public class TaskHistoryRequest {

    private Long taskId;
    private String action;
    private String tituloSnapshot;
    private String descricaoSnapshot;
    private Boolean completedSnapshot;

    public TaskHistoryRequest() {
    }

    public TaskHistoryRequest(Long taskId, String action, String tituloSnapshot,
                               String descricaoSnapshot, Boolean completedSnapshot) {
        this.taskId = taskId;
        this.action = action;
        this.tituloSnapshot = tituloSnapshot;
        this.descricaoSnapshot = descricaoSnapshot;
        this.completedSnapshot = completedSnapshot;
    }

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
}
