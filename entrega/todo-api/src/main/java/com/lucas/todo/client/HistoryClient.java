package com.lucas.todo.client;

import com.lucas.todo.client.dto.TaskHistoryRequest;
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
 */
@FeignClient(name = "history-service")
public interface HistoryClient {

    @PostMapping("/api/history")
    TaskHistoryResponse registrarEvento(@RequestBody TaskHistoryRequest request);

    @GetMapping("/api/history/task/{taskId}")
    List<TaskHistoryResponse> buscarHistorico(@PathVariable("taskId") Long taskId);

    @GetMapping("/api/history/task/{taskId}/estatisticas")
    TaskStatsResponse buscarEstatisticas(@PathVariable("taskId") Long taskId);
}
