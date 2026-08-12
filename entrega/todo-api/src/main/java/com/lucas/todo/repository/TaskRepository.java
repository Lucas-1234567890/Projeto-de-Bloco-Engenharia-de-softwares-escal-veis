package com.lucas.todo.repository;

import com.lucas.todo.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByCompleted(boolean completed, Pageable pageable);

    Page<Task> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
}
