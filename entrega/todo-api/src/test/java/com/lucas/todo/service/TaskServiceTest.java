package com.lucas.todo.service;

import com.lucas.todo.client.HistoryClient;
import com.lucas.todo.client.dto.TaskHistoryRequest;
import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.exception.TaskNotFoundException;
import com.lucas.todo.model.Task;
import com.lucas.todo.repository.TaskRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(repository, historyClient);
    }

    @Test
    void criarDeveSalvarTaskERegistrarEventoNoHistoryService() {
        Task task = new Task("Nova tarefa", "descrição");
        task.setId(1L);
        when(repository.save(any(Task.class))).thenReturn(task);

        Task resultado = service.criar(task);

        assertThat(resultado.getId()).isEqualTo(1L);

        ArgumentCaptor<TaskHistoryRequest> captor = ArgumentCaptor.forClass(TaskHistoryRequest.class);
        verify(historyClient).registrarEvento(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("CREATED");
        assertThat(captor.getValue().getTaskId()).isEqualTo(1L);
    }

    @Test
    void concluirDeveMarcarCompletedERegistrarEventoDeConclusao() {
        Task existente = new Task("Tarefa", null);
        existente.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task resultado = service.concluir(5L);

        assertThat(resultado.isCompleted()).isTrue();

        ArgumentCaptor<TaskHistoryRequest> captor = ArgumentCaptor.forClass(TaskHistoryRequest.class);
        verify(historyClient).registrarEvento(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("COMPLETED");
        assertThat(captor.getValue().getCompletedSnapshot()).isTrue();
    }

    @Test
    void buscarPorIdDeveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deletarDeveRegistrarEventoAntesDeApagar() {
        Task existente = new Task("Tarefa a apagar", null);
        existente.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));

        service.deletar(7L);

        ArgumentCaptor<TaskHistoryRequest> captor = ArgumentCaptor.forClass(TaskHistoryRequest.class);
        verify(historyClient).registrarEvento(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("DELETED");
        verify(repository).delete(existente);
    }

    @Test
    void criarNaoDeveFalharQuandoHistoryServiceIndisponivel() {
        // Histórico é auditoria auxiliar: indisponibilidade do microsserviço
        // não pode impedir a criação da task.
        Task task = new Task("Nova tarefa", "descrição");
        task.setId(1L);
        when(repository.save(any(Task.class))).thenReturn(task);
        when(historyClient.registrarEvento(any())).thenThrow(mock(FeignException.class));

        Task resultado = service.criar(task);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(historyClient).registrarEvento(any());
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
