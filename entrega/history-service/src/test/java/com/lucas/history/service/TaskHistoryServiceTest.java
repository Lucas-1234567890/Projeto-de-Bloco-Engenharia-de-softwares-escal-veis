package com.lucas.history.service;

import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.exception.HistoricoNaoEncontradoException;
import com.lucas.history.messaging.TaskEventMessage;
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
import java.util.UUID;

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

    private TaskEventMessage mensagem(UUID eventId, String tipo, LocalDateTime occurredAt) {
        return new TaskEventMessage(eventId, tipo, 1L, "Nova tarefa", "desc", false, occurredAt);
    }

    @Test
    void registrarDeveSalvarEventoComActionConvertidaDeString() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);

        boolean gravou = service.registrar(mensagem(eventId, "CREATED", LocalDateTime.of(2026, 9, 20, 10, 0)));

        assertThat(gravou).isTrue();
        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(repository).save(captor.capture());
        TaskHistory salvo = captor.getValue();
        assertThat(salvo.getEventId()).isEqualTo(eventId);
        assertThat(salvo.getTaskId()).isEqualTo(1L);
        assertThat(salvo.getAction()).isEqualTo(TaskAction.CREATED);
        assertThat(salvo.getTituloSnapshot()).isEqualTo("Nova tarefa");
    }

    @Test
    void registrarDeveUsarOMomentoEmQueOEventoOcorreuEnaoOdeChegada() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime ocorridoHaMuitoTempo = LocalDateTime.of(2026, 1, 1, 8, 0);
        when(repository.existsByEventId(eventId)).thenReturn(false);

        service.registrar(mensagem(eventId, "COMPLETED", ocorridoHaMuitoTempo));

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getChangedAt()).isEqualTo(ocorridoHaMuitoTempo);
    }

    @Test
    void registrarDeveIgnorarEventoDuplicado() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(true);

        boolean gravou = service.registrar(mensagem(eventId, "CREATED", LocalDateTime.now()));

        assertThat(gravou).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void registrarDeveRejeitarTipoDeEventoDesconhecido() {
        // Vira "poison message": após os retries, o RabbitMQ a envia para a DLQ.
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);

        assertThatThrownBy(() -> service.registrar(mensagem(eventId, "EXPLODED", LocalDateTime.now())))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void registrarDeveRejeitarEventoSemEventId() {
        assertThatThrownBy(() -> service.registrar(mensagem(null, "CREATED", LocalDateTime.now())))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
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
