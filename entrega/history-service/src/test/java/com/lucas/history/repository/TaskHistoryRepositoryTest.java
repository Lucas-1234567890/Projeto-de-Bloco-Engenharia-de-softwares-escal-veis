package com.lucas.history.repository;

import com.lucas.history.model.TaskAction;
import com.lucas.history.model.TaskHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    @Test
    void existsByEventIdDeveDistinguirEventoJaRegistrado() {
        UUID eventId = UUID.randomUUID();
        TaskHistory h = evento(30L, TaskAction.CREATED, LocalDateTime.now());
        h.setEventId(eventId);
        repository.save(h);

        assertThat(repository.existsByEventId(eventId)).isTrue();
        assertThat(repository.existsByEventId(UUID.randomUUID())).isFalse();
    }

    @Test
    void deveAceitarVariasLinhasSemEventIdPorCausaDosRegistrosAntigos() {
        // Linhas gravadas antes do TP4 não têm event_id; o índice único não pode barrá-las.
        repository.saveAndFlush(evento(40L, TaskAction.CREATED, LocalDateTime.now()));
        repository.saveAndFlush(evento(41L, TaskAction.CREATED, LocalDateTime.now()));

        assertThat(repository.findByTaskIdOrderByChangedAtDesc(40L)).hasSize(1);
        assertThat(repository.findByTaskIdOrderByChangedAtDesc(41L)).hasSize(1);
    }
}
