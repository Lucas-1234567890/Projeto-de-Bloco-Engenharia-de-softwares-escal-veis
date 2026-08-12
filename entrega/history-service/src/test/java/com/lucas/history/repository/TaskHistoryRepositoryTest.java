package com.lucas.history.repository;

import com.lucas.history.model.TaskAction;
import com.lucas.history.model.TaskHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskHistoryRepositoryTest {

    @Autowired
    private TaskHistoryRepository repository;

    private TaskHistory evento(Long taskId, TaskAction action, LocalDateTime changedAt) {
        TaskHistory h = new TaskHistory();
        h.setTaskId(taskId);
        h.setAction(action);
        h.setTituloSnapshot("Tarefa " + taskId);
        h.setCompletedSnapshot(action == TaskAction.COMPLETED);
        h.setChangedAt(changedAt);
        return h;
    }

    @Test
    void deveGerarIdAoSalvar() {
        TaskHistory salvo = repository.save(evento(1L, TaskAction.CREATED, LocalDateTime.now()));

        assertThat(salvo.getId()).isNotNull();
    }

    @Test
    void deveOrdenarPorChangedAtDescendente() {
        LocalDateTime agora = LocalDateTime.now();
        repository.save(evento(10L, TaskAction.CREATED, agora.minusMinutes(10)));
        repository.save(evento(10L, TaskAction.UPDATED, agora.minusMinutes(5)));
        repository.save(evento(10L, TaskAction.COMPLETED, agora));

        List<TaskHistory> resultado = repository.findByTaskIdOrderByChangedAtDesc(10L);

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).getAction()).isEqualTo(TaskAction.COMPLETED);
        assertThat(resultado.get(2).getAction()).isEqualTo(TaskAction.CREATED);
    }

    @Test
    void deveOrdenarPorChangedAtAscendente() {
        LocalDateTime agora = LocalDateTime.now();
        repository.save(evento(20L, TaskAction.COMPLETED, agora));
        repository.save(evento(20L, TaskAction.CREATED, agora.minusMinutes(10)));

        List<TaskHistory> resultado = repository.findByTaskIdOrderByChangedAtAsc(20L);

        assertThat(resultado.get(0).getAction()).isEqualTo(TaskAction.CREATED);
        assertThat(resultado.get(1).getAction()).isEqualTo(TaskAction.COMPLETED);
    }

    @Test
    void deveRetornarVazioQuandoTaskSemEventos() {
        List<TaskHistory> resultado = repository.findByTaskIdOrderByChangedAtDesc(999L);

        assertThat(resultado).isEmpty();
    }
}
