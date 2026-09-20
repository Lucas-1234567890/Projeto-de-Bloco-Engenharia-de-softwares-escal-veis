package com.lucas.history.service;

import com.lucas.history.dto.TaskHistoryResponse;
import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.exception.HistoricoNaoEncontradoException;
import com.lucas.history.messaging.TaskEventMessage;
import com.lucas.history.model.TaskAction;
import com.lucas.history.model.TaskHistory;
import com.lucas.history.repository.TaskHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TaskHistoryService.class);

    private final TaskHistoryRepository repository;

    public TaskHistoryService(TaskHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra um evento recebido do RabbitMQ.
     * <p>
     * <b>Consumidor idempotente:</b> o broker entrega <i>at-least-once</i>
     * (um redelivery após falha de ack, por exemplo, gera a mesma mensagem
     * duas vezes). O {@code eventId} identifica o evento; se já foi
     * registrado, ele é ignorado em vez de duplicar o histórico.
     * <p>
     * Exceções aqui (evento malformado, tipo desconhecido) acionam o
     * retry e, depois, a Dead Letter Queue.
     *
     * @return {@code true} se gravou, {@code false} se era duplicata
     */
    @Transactional
    public boolean registrar(TaskEventMessage event) {
        if (event.eventId() == null) {
            // Sem eventId não há como garantir idempotência: trata como mensagem inválida.
            throw new IllegalArgumentException("eventId é obrigatório");
        }
        if (event.taskId() == null) {
            throw new IllegalArgumentException("taskId é obrigatório");
        }
        if (repository.existsByEventId(event.eventId())) {
            log.info("Evento {} já processado, ignorando duplicata (taskId={})",
                    event.eventId(), event.taskId());
            return false;
        }

        TaskHistory history = new TaskHistory();
        history.setEventId(event.eventId());
        history.setTaskId(event.taskId());
        // valueOf lança IllegalArgumentException para tipo desconhecido -> DLQ
        history.setAction(TaskAction.valueOf(event.eventType()));
        history.setTituloSnapshot(event.titulo());
        history.setDescricaoSnapshot(event.descricao());
        history.setCompletedSnapshot(event.completed());
        // Usa o momento em que o fato OCORREU, não o momento em que a mensagem chegou:
        // as métricas (tempo até conclusão) não podem depender da latência da fila.
        history.setChangedAt(event.occurredAt() != null ? event.occurredAt() : LocalDateTime.now());

        repository.save(history);
        log.info("Histórico registrado: taskId={} action={} eventId={}",
                event.taskId(), event.eventType(), event.eventId());
        return true;
    }

    public List<TaskHistoryResponse> buscarPorTask(Long taskId) {
        List<TaskHistory> eventos = repository.findByTaskIdOrderByChangedAtDesc(taskId);
        if (eventos.isEmpty()) {
            throw new HistoricoNaoEncontradoException(taskId);
        }
        return eventos.stream().map(this::toResponse).toList();
    }

    public TaskStatsResponse estatisticas(Long taskId) {
        List<TaskHistory> eventos = repository.findByTaskIdOrderByChangedAtAsc(taskId);
        if (eventos.isEmpty()) {
            throw new HistoricoNaoEncontradoException(taskId);
        }

        LocalDateTime criadoEm = eventos.stream()
                .filter(e -> e.getAction() == TaskAction.CREATED)
                .map(TaskHistory::getChangedAt)
                .findFirst()
                .orElse(null);

        List<LocalDateTime> conclusoes = eventos.stream()
                .filter(e -> e.getAction() == TaskAction.COMPLETED)
                .map(TaskHistory::getChangedAt)
                .toList();

        LocalDateTime primeiraConclusao = conclusoes.isEmpty() ? null : conclusoes.get(0);

        Long tempoAteConclusaoSegundos = (criadoEm != null && primeiraConclusao != null)
                ? Duration.between(criadoEm, primeiraConclusao).getSeconds()
                : null;

        // Reabertura = task foi concluída mais de uma vez (concluir -> atualizar -> concluir de novo)
        int reaberturas = Math.max(0, conclusoes.size() - 1);

        TaskStatsResponse stats = new TaskStatsResponse();
        stats.setTaskId(taskId);
        stats.setTotalEventos(eventos.size());
        stats.setCriadoEm(criadoEm);
        stats.setConcluidoEm(primeiraConclusao);
        stats.setTempoAteConclusaoSegundos(tempoAteConclusaoSegundos);
        stats.setQuantidadeConclusoes(conclusoes.size());
        stats.setReaberturas(reaberturas);
        return stats;
    }

    private TaskHistoryResponse toResponse(TaskHistory h) {
        TaskHistoryResponse r = new TaskHistoryResponse();
        r.setId(h.getId());
        r.setTaskId(h.getTaskId());
        r.setAction(h.getAction().name());
        r.setTituloSnapshot(h.getTituloSnapshot());
        r.setDescricaoSnapshot(h.getDescricaoSnapshot());
        r.setCompletedSnapshot(h.getCompletedSnapshot());
        r.setChangedAt(h.getChangedAt());
        return r;
    }
}
