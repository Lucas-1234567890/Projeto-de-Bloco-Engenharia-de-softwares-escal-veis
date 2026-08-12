package com.lucas.history.repository;

import com.lucas.history.model.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(Long taskId);

    List<TaskHistory> findByTaskIdOrderByChangedAtAsc(Long taskId);
}
