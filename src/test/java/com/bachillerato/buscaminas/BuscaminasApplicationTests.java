package com.bachillerato.buscaminas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de contexto: levanta la aplicación completa contra MySQL.
 * Requiere la base «minas» creada, por eso solo corre si MINAS_IT=true:
 *   MINAS_IT=true mvn test
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MINAS_IT", matches = "true")
class BuscaminasApplicationTests {

    @Test
    void contextLoads() {
    }
}
