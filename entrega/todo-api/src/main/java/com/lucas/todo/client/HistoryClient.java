package com.lucas.todo.client;

import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.client.dto.TaskStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Client declarativo do microsserviço history-service.
 * <p>
 * O "name" abaixo é o mesmo {@code spring.application.name} configurado no
 * history-service — o Feign pergunta ao Eureka o endereço atual desse nome
 * em tempo de execução, então nenhuma URL fica hardcoded aqui.
 * <p>
 * <b>TP4:</b> este client agora serve só para <i>consultas</i> (queries). A
 * escrita do histórico deixou de ser uma chamada REST síncrona: o todo-api
 * publica eventos no RabbitMQ e o history-service os consome de forma
 * assíncrona (ver {@code messaging/TaskEventPublisher}).
 */
@FeignClient(name = "history-service")
public interface HistoryClient {

    @GetMapping("/api/history/task/{taskId}")
    List<TaskHistoryResponse> buscarHistorico(@PathVariable("taskId") Long taskId);

    @GetMapping("/api/history/task/{taskId}/estatisticas")
    TaskStatsResponse buscarEstatisticas(@PathVariable("taskId") Long taskId);
}
