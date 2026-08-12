package com.lucas.history.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Snapshot imutável do estado de uma Task no momento de um evento.
 * <p>
 * Esta é a entidade dona da tabela {@code task_history}, que antes vivia
 * dentro do monólito (todo-api) e agora pertence a este microsserviço.
 * {@code taskId} não é FK física de propósito: o histórico precisa
 * sobreviver mesmo depois que a task original é deletada no todo-api —
 * e, num cenário de microsserviços, nem faria sentido ter FK para uma
 * tabela que mora em outro banco de dados.
 */
@Entity
@Table(name = "task_history")
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskAction action;

    @Column(name = "titulo_snapshot", length = 200)
    private String tituloSnapshot;

    @Column(name = "descricao_snapshot", length = 1000)
    private String descricaoSnapshot;

    @Column(name = "completed_snapshot")
    private Boolean completedSnapshot;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public TaskHistory() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public TaskAction getAction() { return action; }
    public void setAction(TaskAction action) { this.action = action; }

    public String getTituloSnapshot() { return tituloSnapshot; }
    public void setTituloSnapshot(String tituloSnapshot) { this.tituloSnapshot = tituloSnapshot; }

    public String getDescricaoSnapshot() { return descricaoSnapshot; }
    public void setDescricaoSnapshot(String descricaoSnapshot) { this.descricaoSnapshot = descricaoSnapshot; }

    public Boolean getCompletedSnapshot() { return completedSnapshot; }
    public void setCompletedSnapshot(Boolean completedSnapshot) { this.completedSnapshot = completedSnapshot; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
