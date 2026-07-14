package com.lucas.todo.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Snapshot imutável do estado de uma Task no momento de um evento.
 * Não referencia Task via @ManyToOne de propósito: precisa sobreviver
 * mesmo depois que a task original é deletada (é literalmente o motivo
 * dessa tabela existir).
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
    private LocalDateTime changedAt = LocalDateTime.now();

    public TaskHistory() {
    }

    public static TaskHistory of(Task task, TaskAction action) {
        TaskHistory history = new TaskHistory();
        history.taskId = task.getId();
        history.action = action;
        history.tituloSnapshot = task.getTitulo();
        history.descricaoSnapshot = task.getDescricao();
        history.completedSnapshot = task.isCompleted();
        history.changedAt = LocalDateTime.now();
        return history;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public TaskAction getAction() { return action; }
    public String getTituloSnapshot() { return tituloSnapshot; }
    public String getDescricaoSnapshot() { return descricaoSnapshot; }
    public Boolean getCompletedSnapshot() { return completedSnapshot; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
