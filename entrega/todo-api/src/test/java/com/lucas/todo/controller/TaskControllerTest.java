package com.lucas.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.todo.client.dto.TaskHistoryResponse;
import com.lucas.todo.client.dto.TaskStatsResponse;
import com.lucas.todo.exception.TaskNotFoundException;
import com.lucas.todo.model.Task;
import com.lucas.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService service;

    @Test
    void deveCriarTaskERetornar201() throws Exception {
        Task task = new Task("Nova tarefa", "descrição");
        task.setId(1L);
        when(service.criar(any(Task.class))).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new Task("Nova tarefa", "descrição"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Nova tarefa"));
    }

    @Test
    void deveRetornar400QuandoTituloEmBranco() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new Task("", "sem titulo"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404QuandoTaskNaoExiste() throws Exception {
        when(service.buscarPorId(999L)).thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveListarComPaginacao() throws Exception {
        Task task = new Task("Tarefa", null);
        task.setId(1L);
        Page<Task> pagina = new PageImpl<>(java.util.List.of(task), PageRequest.of(0, 20), 1);
        when(service.listar(any())).thenReturn(pagina);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Tarefa"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveRetornarHistoricoDaTask() throws Exception {
        TaskHistoryResponse evento = new TaskHistoryResponse();
        evento.setTaskId(1L);
        evento.setAction("CREATED");
        when(service.historico(1L)).thenReturn(java.util.List.of(evento));

        mockMvc.perform(get("/api/tasks/1/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATED"));
    }

    @Test
    void deveRetornar404NoHistoricoQuandoTaskNuncaExistiu() throws Exception {
        when(service.historico(999L)).thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(get("/api/tasks/999/historico"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornarEstatisticasDaTask() throws Exception {
        TaskStatsResponse stats = new TaskStatsResponse();
        stats.setTaskId(1L);
        stats.setTotalEventos(2);
        when(service.estatisticas(1L)).thenReturn(stats);

        mockMvc.perform(get("/api/tasks/1/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(2));
    }
}
