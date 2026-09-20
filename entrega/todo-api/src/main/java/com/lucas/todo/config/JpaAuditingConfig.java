package com.lucas.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Isolado da classe principal (TodoApplication) de propósito: @WebMvcTest usa
 * a classe anotada com @SpringBootConfiguration como fonte de configuração, e
 * qualquer anotação presente diretamente nela é processada mesmo dentro da
 * fatia de teste. Com @EnableJpaAuditing na própria TodoApplication, o
 * WebMvcTest tentava criar o bean jpaAuditingHandler sem nenhum metamodelo
 * JPA carregado (a fatia não inclui entidades/repositórios), falhando com
 * "JPA metamodel must not be empty". Movendo para uma @Configuration à
 * parte, o filtro de componentes do WebMvcTest a exclui normalmente.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
