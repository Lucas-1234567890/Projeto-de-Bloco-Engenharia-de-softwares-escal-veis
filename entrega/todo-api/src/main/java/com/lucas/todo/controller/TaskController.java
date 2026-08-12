package com.lucas.todo.controller;

import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.client.dto.TaskStatsResponse;
import com.lucas.todo.model.Task;
import com.lucas.todo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public Page<Task> listar(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String titulo,
            Pageable pageable) {
        if (completed != null) {
            return service.listarPorStatus(completed, pageable);
        }
        if (titulo != null && !titulo.isBlank()) {
            return service.buscarPorTitulo(titulo, pageable);
        }
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public Task buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task criar(@Valid @RequestBody Task task) {
        return service.criar(task);
    }

    @PutMapping("/{id}")
    public Task atualizar(@PathVariable Long id, @Valid @RequestBody Task task) {
        return service.atualizar(id, task);
    }

    @PatchMapping("/{id}/concluir")
    public Task concluir(@PathVariable Long id) {
        return service.concluir(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }

    /** Endpoint de acesso ao microsserviço history-service via este mesmo host. */
    @GetMapping("/{id}/historico")
    public List<TaskHistoryResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }

    /** Métricas derivadas do histórico (tempo até conclusão, reaberturas...), via history-service. */
    @GetMapping("/{id}/estatisticas")
    public TaskStatsResponse estatisticas(@PathVariable Long id) {
        return service.estatisticas(id);
    }
}
