package com.lucas.todo.service;

import com.lucas.todo.client.HistoryClient;
import com.lucas.todo.client.dto.TaskHistoryRequest;
import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.client.dto.TaskStatsResponse;
import com.lucas.todo.exception.TaskNotFoundException;
import com.lucas.todo.model.Task;
import com.lucas.todo.model.TaskAction;
import com.lucas.todo.repository.TaskRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository repository;
    private final HistoryClient historyClient;

    public TaskService(TaskRepository repository, HistoryClient historyClient) {
        this.repository = repository;
        this.historyClient = historyClient;
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
        registrarHistorico(salva, TaskAction.CREATED);
        return salva;
    }

    @Transactional
    public Task atualizar(Long id, Task dadosAtualizados) {
        Task task = buscarPorId(id);
        task.setTitulo(dadosAtualizados.getTitulo());
        task.setDescricao(dadosAtualizados.getDescricao());
        Task atualizada = repository.save(task);
        registrarHistorico(atualizada, TaskAction.UPDATED);
        return atualizada;
    }

    @Transactional
    public Task concluir(Long id) {
        Task task = buscarPorId(id);
        task.setCompleted(true);
        Task concluida = repository.save(task);
        registrarHistorico(concluida, TaskAction.COMPLETED);
        return concluida;
    }

    @Transactional
    public void deletar(Long id) {
        Task task = buscarPorId(id);
        // Grava o snapshot final ANTES de apagar — é o registro de que a task existiu.
        registrarHistorico(task, TaskAction.DELETED);
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
     * Envia o evento para o history-service. Propositalmente não deixa uma
     * falha de comunicação com o microsserviço derrubar a operação principal
     * na task: histórico é auditoria auxiliar, não deve travar o CRUD.
     */
    private void registrarHistorico(Task task, TaskAction action) {
        try {
            TaskHistoryRequest request = new TaskHistoryRequest(
                    task.getId(),
                    action.name(),
                    task.getTitulo(),
                    task.getDescricao(),
                    task.isCompleted()
            );
            historyClient.registrarEvento(request);
        } catch (FeignException ex) {
            log.warn("Falha ao registrar histórico da task {} (ação={}): {}",
                    task.getId(), action, ex.getMessage());
        }
    }
}
