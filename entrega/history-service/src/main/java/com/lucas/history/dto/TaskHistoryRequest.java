package com.lucas.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Contrato de entrada usado pelo todo-api para registrar um evento.
 * Propositalmente não reutiliza nenhuma classe do todo-api: cada
 * microsserviço define o próprio contrato (DTO), evitando acoplamento
 * via biblioteca compartilhada.
 */
public class TaskHistoryRequest {

    @NotNull(message = "taskId é obrigatório")
    private Long taskId;

    @NotBlank(message = "action é obrigatório")
    private String action;

    private String tituloSnapshot;

    private String descricaoSnapshot;

    private Boolean completedSnapshot;

    public TaskHistoryRequest() {
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
