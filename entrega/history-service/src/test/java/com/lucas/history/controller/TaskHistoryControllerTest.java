package com.lucas.history.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.history.dto.TaskHistoryRequest;
import com.lucas.history.dto.TaskHistoryResponse;
import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.exception.HistoricoNaoEncontradoException;
import com.lucas.history.service.TaskHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskHistoryController.class)
class TaskHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskHistoryService service;

    @Test
    void deveRegistrarEventoERetornar201() throws Exception {
        TaskHistoryResponse resposta = new TaskHistoryResponse();
        resposta.setId(1L);
        resposta.setTaskId(5L);
        resposta.setAction("CREATED");
        when(service.registrar(any())).thenReturn(resposta);

        TaskHistoryRequest request = new TaskHistoryRequest();
        request.setTaskId(5L);
        request.setAction("CREATED");
        request.setTituloSnapshot("Tarefa");

        mockMvc.perform(post("/api/history")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(5))
                .andExpect(jsonPath("$.action").value("CREATED"));
    }

    @Test
    void deveRetornar400QuandoTaskIdAusente() throws Exception {
        TaskHistoryRequest request = new TaskHistoryRequest();
        request.setAction("CREATED");

        mockMvc.perform(post("/api/history")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarHistoricoDaTask() throws Exception {
        TaskHistoryResponse evento = new TaskHistoryResponse();
        evento.setId(1L);
        evento.setTaskId(5L);
        evento.setAction("CREATED");
        when(service.buscarPorTask(5L)).thenReturn(List.of(evento));

        mockMvc.perform(get("/api/history/task/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(5));
    }

    @Test
    void deveRetornar404QuandoTaskSemHistorico() throws Exception {
        when(service.buscarPorTask(999L)).thenThrow(new HistoricoNaoEncontradoException(999L));

        mockMvc.perform(get("/api/history/task/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveRetornarEstatisticas() throws Exception {
        TaskStatsResponse stats = new TaskStatsResponse();
        stats.setTaskId(5L);
        stats.setTotalEventos(3);
        stats.setQuantidadeConclusoes(1);
        when(service.estatisticas(5L)).thenReturn(stats);

        mockMvc.perform(get("/api/history/task/5/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(3));
    }
}
