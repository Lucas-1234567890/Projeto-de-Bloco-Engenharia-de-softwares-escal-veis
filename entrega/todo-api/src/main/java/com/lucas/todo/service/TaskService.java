package com.lucas.todo.service;

import com.lucas.todo.client.HistoryClient;
import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.client.dto.TaskStatsResponse;
import com.lucas.todo.exception.TaskNotFoundException;
import com.lucas.todo.messaging.TaskEventMessage;
import com.lucas.todo.model.Task;
import com.lucas.todo.model.TaskAction;
import com.lucas.todo.repository.TaskRepository;
import feign.FeignException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository repository;
    private final HistoryClient historyClient;
    private final ApplicationEventPublisher eventPublisher;

    public TaskService(TaskRepository repository,
                       HistoryClient historyClient,
                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.historyClient = historyClient;
        this.eventPublisher = eventPublisher;
    }

    public Page<Task> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Task> listarPorStatus(boolean completed, Pageable pageable) {
        return repository.findByCompleted(completed, pageable);
    }

    public Page<Task> buscarPorTitulo(String titulo, Pageable pageable) {
        return repository.findByTituloContainingIgnoreCase(titulo, pageable);
    }

    public Task buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public Task criar(Task task) {
        Task salva = repository.save(task);
        publicarEvento(salva, TaskAction.CREATED);
        return salva;
    }

    @Transactional
    public Task atualizar(Long id, Task dadosAtualizados) {
        Task task = buscarPorId(id);
        task.setTitulo(dadosAtualizados.getTitulo());
        task.setDescricao(dadosAtualizados.getDescricao());
        Task atualizada = repository.save(task);
        publicarEvento(atualizada, TaskAction.UPDATED);
        return atualizada;
    }

    @Transactional
    public Task concluir(Long id) {
        Task task = buscarPorId(id);
        task.setCompleted(true);
        Task concluida = repository.save(task);
        publicarEvento(concluida, TaskAction.COMPLETED);
        return concluida;
    }

    @Transactional
    public void deletar(Long id) {
        Task task = buscarPorId(id);
        // O evento carrega o snapshot final (copiado aqui, antes do delete) e só
        // vai ao broker depois do commit — é o registro de que a task existiu.
        publicarEvento(task, TaskAction.DELETED);
        repository.delete(task);
    }

    /**
     * Busca o histórico de eventos de uma task no history-service.
     * Não valida existência da task atual de propósito: uma task deletada
     * não existe mais em {@code tasks}, mas seu histórico continua consultável.
     */
    public List<TaskHistoryResponse> historico(Long id) {
        try {
            return historyClient.buscarHistorico(id);
        } catch (FeignException.NotFound ex) {
            throw new TaskNotFoundException(id);
        }
    }

    public TaskStatsResponse estatisticas(Long id) {
        try {
            return historyClient.buscarEstatisticas(id);
        } catch (FeignException.NotFound ex) {
            throw new TaskNotFoundException(id);
        }
    }

    /**
     * Emite o evento de domínio. Ele NÃO vai direto ao RabbitMQ: o Spring o
     * guarda e o {@code TaskEventPublisher} só o envia ao broker depois que a
     * transação atual der COMMIT. Assim o CRUD não depende da disponibilidade
     * do history-service nem do broker, e nenhum evento "fantasma" é publicado
     * se houver rollback.
     */
    private void publicarEvento(Task task, TaskAction action) {
        eventPublisher.publishEvent(TaskEventMessage.of(action, task));
    }
}
