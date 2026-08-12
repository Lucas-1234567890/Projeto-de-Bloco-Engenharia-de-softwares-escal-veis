package com.lucas.history.service;

import com.lucas.history.dto.TaskHistoryRequest;
import com.lucas.history.dto.TaskHistoryResponse;
import com.lucas.history.dto.TaskStatsResponse;
import com.lucas.history.exception.HistoricoNaoEncontradoException;
import com.lucas.history.model.TaskAction;
import com.lucas.history.model.TaskHistory;
import com.lucas.history.repository.TaskHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskHistoryService {

    private final TaskHistoryRepository repository;

    public TaskHistoryService(TaskHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TaskHistoryResponse registrar(TaskHistoryRequest request) {
        TaskHistory history = new TaskHistory();
        history.setTaskId(request.getTaskId());
        history.setAction(TaskAction.valueOf(request.getAction()));
        history.setTituloSnapshot(request.getTituloSnapshot());
        history.setDescricaoSnapshot(request.getDescricaoSnapshot());
        history.setCompletedSnapshot(request.getCompletedSnapshot());
        history.setChangedAt(LocalDateTime.now());

        TaskHistory salvo = repository.save(history);
        return toResponse(salvo);
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
