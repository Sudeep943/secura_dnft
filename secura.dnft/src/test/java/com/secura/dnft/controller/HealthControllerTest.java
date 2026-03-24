package com.secura.dnft.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void rootEndpointReturnsUpStatus() {
        HealthController controller = new HealthController();
        Map<String, String> response = controller.root();
        assertEquals("UP", response.get("status"));
    }
}
