package com.lucas.todo.repository;

import com.lucas.todo.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository repository;

    @Test
    void deveGerarIdECamposDeAuditoriaAoSalvar() {
        Task task = repository.save(new Task("Estudar JPA", "Revisar mapeamento objeto-relacional"));

        assertThat(task.getId()).isNotNull();
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isNotNull();
    }

    @Test
    void deveFiltrarPorStatusComPaginacao() {
        repository.save(new Task("Tarefa pendente 1", null));
        repository.save(new Task("Tarefa pendente 2", null));
        Task concluida = new Task("Tarefa concluída", null);
        concluida.setCompleted(true);
        repository.save(concluida);

        Page<Task> pendentes = repository.findByCompleted(false, PageRequest.of(0, 10));
        Page<Task> concluidas = repository.findByCompleted(true, PageRequest.of(0, 10));

        assertThat(pendentes.getTotalElements()).isEqualTo(2);
        assertThat(concluidas.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deveBuscarPorTituloIgnorandoCaixa() {
        repository.save(new Task("Revisar contrato de dados", null));
        repository.save(new Task("Comprar café", null));

        Page<Task> resultado = repository.findByTituloContainingIgnoreCase("REVISAR", PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getTitulo()).contains("Revisar");
    }
}
