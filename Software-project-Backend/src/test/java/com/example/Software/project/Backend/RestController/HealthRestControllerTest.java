package com.example.Software.project.Backend.RestController;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthRestControllerTest {

    @Test
    void healthReportsUp() {
        Map<String, String> response = new HealthRestController().health();

        assertEquals("UP", response.get("status"));
    }
}
