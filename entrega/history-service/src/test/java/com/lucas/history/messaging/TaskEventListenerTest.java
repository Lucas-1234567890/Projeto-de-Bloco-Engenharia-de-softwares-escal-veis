package com.lucas.history.messaging;

import com.lucas.history.service.TaskHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskEventListenerTest {

    @Mock
    private TaskHistoryService service;

    @InjectMocks
    private TaskEventListener listener;

    @Test
    void deveDelegarEventoRecebidoParaOService() {
        TaskEventMessage evento = new TaskEventMessage(
                UUID.randomUUID(), "CREATED", 1L, "Tarefa", null, false, LocalDateTime.now());

        listener.aoReceberEvento(evento);

        verify(service).registrar(evento);
    }
}
