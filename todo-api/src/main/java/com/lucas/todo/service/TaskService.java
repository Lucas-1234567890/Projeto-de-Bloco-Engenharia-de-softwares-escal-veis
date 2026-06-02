package com.lucas.todo.service;

import com.lucas.todo.model.Task;
import com.lucas.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> listarTodas() {
        return repository.findAll();
    }

    public Task criar(Task task) {
        return repository.save(task);
    }

    public Task concluir(Long id) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));
        task.setCompleted(true);
        return repository.save(task);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
}
