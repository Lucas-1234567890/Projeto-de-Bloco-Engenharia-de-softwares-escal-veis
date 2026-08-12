package com.lucas.history.service;

import com.lucas.history.dto.TaskHistoryRequest;
import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.exception.HistoricoNaoEncontradoException;
import com.lucas.history.model.TaskAction;
import com.lucas.history.model.TaskHistory;
import com.lucas.history.repository.TaskHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHistoryServiceTest {

    @Mock
    private TaskHistoryRepository repository;

    private TaskHistoryService service;

    @BeforeEach
    void setUp() {
        service = new TaskHistoryService(repository);
    }

    private TaskHistory evento(Long id, TaskAction action, LocalDateTime changedAt) {
        TaskHistory h = new TaskHistory();
        h.setId(id);
        h.setTaskId(1L);
        h.setAction(action);
        h.setTituloSnapshot("Tarefa");
        h.setCompletedSnapshot(action == TaskAction.COMPLETED);
        h.setChangedAt(changedAt);
        return h;
    }

    @Test
    void registrarDeveSalvarComActionConvertidaDeString() {
        TaskHistoryRequest request = new TaskHistoryRequest();
        request.setTaskId(1L);
        request.setAction("CREATED");
        request.setTituloSnapshot("Nova tarefa");
        request.setCompletedSnapshot(false);

        when(repository.save(any(TaskHistory.class))).thenAnswer(inv -> {
            TaskHistory h = inv.getArgument(0);
            h.setId(100L);
            return h;
        });

        var resultado = service.registrar(request);

        assertThat(resultado.getId()).isEqualTo(100L);
        assertThat(resultado.getAction()).isEqualTo("CREATED");

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(TaskAction.CREATED);
    }

    @Test
    void buscarPorTaskDeveLancarExcecaoQuandoNaoHaEventos() {
        when(repository.findByTaskIdOrderByChangedAtDesc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.buscarPorTask(1L))
                .isInstanceOf(HistoricoNaoEncontradoException.class);
    }

    @Test
    void estatisticasDeveCalcularTempoAteConclusaoEReaberturas() {
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime concluidoPrimeiraVez = criado.plusHours(2);
        LocalDateTime reaberto = concluidoPrimeiraVez.plusHours(1);
        LocalDateTime concluidoDeNovo = reaberto.plusMinutes(30);

        when(repository.findByTaskIdOrderByChangedAtAsc(1L)).thenReturn(List.of(
                evento(1L, TaskAction.CREATED, criado),
                evento(2L, TaskAction.COMPLETED, concluidoPrimeiraVez),
                evento(3L, TaskAction.UPDATED, reaberto),
                evento(4L, TaskAction.COMPLETED, concluidoDeNovo)
        ));

        TaskStatsResponse stats = service.estatisticas(1L);

        assertThat(stats.getTotalEventos()).isEqualTo(4);
        assertThat(stats.getCriadoEm()).isEqualTo(criado);
        assertThat(stats.getConcluidoEm()).isEqualTo(concluidoPrimeiraVez);
        assertThat(stats.getTempoAteConclusaoSegundos()).isEqualTo(2 * 3600);
        assertThat(stats.getQuantidadeConclusoes()).isEqualTo(2);
        assertThat(stats.getReaberturas()).isEqualTo(1);
    }

    @Test
    void estatisticasDeveLancarExcecaoQuandoNaoHaEventos() {
        when(repository.findByTaskIdOrderByChangedAtAsc(999L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.estatisticas(999L))
                .isInstanceOf(HistoricoNaoEncontradoException.class);
    }
}
