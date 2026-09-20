package com.lucas.history.controller;

import com.lucas.history.dto.TaskHistoryResponse;
import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.service.TaskHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TP4: este controller expõe apenas CONSULTAS. A escrita do histórico deixou de ser
 * um POST síncrono do todo-api — agora chega como evento via RabbitMQ (ver
 * {@code messaging/TaskEventListener}).
 */
@RestController
@RequestMapping("/api/history")
public class TaskHistoryController {

    private final TaskHistoryService service;

    public TaskHistoryController(TaskHistoryService service) {
        this.service = service;
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
