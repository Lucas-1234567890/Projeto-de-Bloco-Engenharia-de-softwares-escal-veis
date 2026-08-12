package com.lucas.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de descoberta de serviços (Eureka).
 * <p>
 * Cada microsserviço (todo-api, history-service) se registra aqui ao subir,
 * informando nome e endereço. Assim ninguém precisa hardcodar
 * "http://localhost:8082" no código: basta perguntar ao Eureka "onde está
 * o history-service agora?" — o que passa a ser configuração, não código.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
