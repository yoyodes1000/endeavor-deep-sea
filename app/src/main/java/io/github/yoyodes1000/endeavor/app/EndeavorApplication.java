package io.github.yoyodes1000.endeavor.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application locale.
 *
 * <p>Un seul processus sert l'API et l'interface Angular compilée, ce qui évite
 * un second serveur et toute configuration CORS.
 */
@SpringBootApplication
public class EndeavorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EndeavorApplication.class, args);
    }
}
