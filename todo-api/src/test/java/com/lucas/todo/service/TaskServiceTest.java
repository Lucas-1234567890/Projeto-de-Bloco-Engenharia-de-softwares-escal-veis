package com.lucas.todo.service;

import com.lucas.todo.exception.TaskNotFoundException;
import com.lucas.todo.model.Task;
import com.lucas.todo.model.TaskAction;
import com.lucas.todo.model.TaskHistory;
import com.lucas.todo.repository.TaskHistoryRepository;
import com.lucas.todo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @Mock
    private TaskHistoryRepository historyRepository;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(repository, historyRepository);
    }

    @Test
    void criarDeveSalvarTaskERegistrarHistoricoDeCriacao() {
        Task task = new Task("Nova tarefa", "descrição");
        task.setId(1L);
        when(repository.save(any(Task.class))).thenReturn(task);

        Task resultado = service.criar(task);

        assertThat(resultado.getId()).isEqualTo(1L);

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(TaskAction.CREATED);
        assertThat(captor.getValue().getTaskId()).isEqualTo(1L);
    }

    @Test
    void concluirDeveMarcarCompletedERegistrarHistoricoDeConclusao() {
        Task existente = new Task("Tarefa", null);
        existente.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task resultado = service.concluir(5L);

        assertThat(resultado.isCompleted()).isTrue();

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(TaskAction.COMPLETED);
        assertThat(captor.getValue().getCompletedSnapshot()).isTrue();
    }

    @Test
    void buscarPorIdDeveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deletarDeveRegistrarHistoricoAntesDeApagar() {
        Task existente = new Task("Tarefa a apagar", null);
        existente.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));

        service.deletar(7L);

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(TaskAction.DELETED);
        verify(repository).delete(existente);
    }

    @Test
    void historicoDeveLancarExcecaoQuandoNaoHaEventosParaOId() {
        when(historyRepository.findByTaskIdOrderByChangedAtDesc(123L)).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> service.historico(123L))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
