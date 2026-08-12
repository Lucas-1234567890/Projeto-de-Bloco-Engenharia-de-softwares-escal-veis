package com.lucas.history.controller;

import com.lucas.history.dto.TaskHistoryRequest;
import com.lucas.history.dto.TaskHistoryResponse;
import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.service.TaskHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class TaskHistoryController {

    private final TaskHistoryService service;

    public TaskHistoryController(TaskHistoryService service) {
        this.service = service;
    }

    /** Chamado pelo todo-api toda vez que uma task é criada/atualizada/concluída/deletada. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskHistoryResponse registrar(@Valid @RequestBody TaskHistoryRequest request) {
        return service.registrar(request);
    }

    @GetMapping("/task/{taskId}")
    public List<TaskHistoryResponse> buscarPorTask(@PathVariable Long taskId) {
        return service.buscarPorTask(taskId);
    }

    @GetMapping("/task/{taskId}/estatisticas")
    public TaskStatsResponse estatisticas(@PathVariable Long taskId) {
        return service.estatisticas(taskId);
    }
}
