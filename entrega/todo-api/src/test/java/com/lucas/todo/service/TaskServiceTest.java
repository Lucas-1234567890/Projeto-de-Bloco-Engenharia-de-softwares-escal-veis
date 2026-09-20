package com.lucas.todo.service;

import com.lucas.todo.client.HistoryClient;
import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.exception.TaskNotFoundException;
import com.lucas.todo.messaging.TaskEventMessage;
import com.lucas.todo.model.Task;
import com.lucas.todo.repository.TaskRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @Mock
    private HistoryClient historyClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(repository, historyClient, eventPublisher);
    }

    private TaskEventMessage eventoPublicado() {
        ArgumentCaptor<TaskEventMessage> captor = ArgumentCaptor.forClass(TaskEventMessage.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void criarDeveSalvarTaskEPublicarEventoCreated() {
        Task task = new Task("Nova tarefa", "descrição");
        task.setId(1L);
        when(repository.save(any(Task.class))).thenReturn(task);

        Task resultado = service.criar(task);

        assertThat(resultado.getId()).isEqualTo(1L);

        TaskEventMessage evento = eventoPublicado();
        assertThat(evento.eventType()).isEqualTo("CREATED");
        assertThat(evento.taskId()).isEqualTo(1L);
        assertThat(evento.titulo()).isEqualTo("Nova tarefa");
        assertThat(evento.eventId()).isNotNull();
        assertThat(evento.occurredAt()).isNotNull();
    }

    @Test
    void criarNaoDeveChamarHistoryServiceDeFormaSincrona() {
        // TP4: a escrita do histórico virou evento assíncrono. O todo-api não
        // pode mais depender de uma chamada REST ao history-service para gravar.
        Task task = new Task("Nova tarefa", "descrição");
        task.setId(1L);
        when(repository.save(any(Task.class))).thenReturn(task);

        service.criar(task);

        verifyNoInteractions(historyClient);
    }

    @Test
    void atualizarDevePublicarEventoUpdated() {
        Task existente = new Task("Antigo", "desc antiga");
        existente.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task resultado = service.atualizar(3L, new Task("Novo título", "nova desc"));

        assertThat(resultado.getTitulo()).isEqualTo("Novo título");

        TaskEventMessage evento = eventoPublicado();
        assertThat(evento.eventType()).isEqualTo("UPDATED");
        assertThat(evento.titulo()).isEqualTo("Novo título");
    }

    @Test
    void concluirDeveMarcarCompletedEPublicarEventoCompleted() {
        Task existente = new Task("Tarefa", null);
        existente.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task resultado = service.concluir(5L);

        assertThat(resultado.isCompleted()).isTrue();

        TaskEventMessage evento = eventoPublicado();
        assertThat(evento.eventType()).isEqualTo("COMPLETED");
        assertThat(evento.completed()).isTrue();
    }

    @Test
    void deletarDevePublicarEventoDeletedComSnapshotEApagar() {
        Task existente = new Task("Tarefa a apagar", null);
        existente.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));

        service.deletar(7L);

        TaskEventMessage evento = eventoPublicado();
        assertThat(evento.eventType()).isEqualTo("DELETED");
        assertThat(evento.taskId()).isEqualTo(7L);
        assertThat(evento.titulo()).isEqualTo("Tarefa a apagar");
        verify(repository).delete(existente);
    }

    @Test
    void atualizarNaoDevePublicarEventoQuandoTaskNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new Task("x", null)))
                .isInstanceOf(TaskNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void buscarPorIdDeveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void historicoDeveDelegarParaHistoryClient() {
        TaskHistoryResponse evento = new TaskHistoryResponse();
        evento.setTaskId(3L);
        when(historyClient.buscarHistorico(3L)).thenReturn(List.of(evento));

        List<TaskHistoryResponse> resultado = service.historico(3L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTaskId()).isEqualTo(3L);
    }

    @Test
    void historicoDeveLancarTaskNotFoundQuandoHistoryServiceRetorna404() {
        when(historyClient.buscarHistorico(999L)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> service.historico(999L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void estatisticasDeveLancarTaskNotFoundQuandoHistoryServiceRetorna404() {
        when(historyClient.buscarEstatisticas(999L)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> service.estatisticas(999L))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
