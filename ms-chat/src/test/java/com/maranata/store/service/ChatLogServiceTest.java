package com.maranata.store.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatLogServiceTest {

    @Test
    void registraLasEntradasMasRecientesPrimero() {
        ChatLogService logs = new ChatLogService();
        logs.registrar("app", "entrada", "hola", "procesado");
        logs.registrar("whatsapp", "salida", "respuesta", "enviado");

        var entradas = logs.listar(10);
        assertEquals(2, entradas.size());
        assertEquals("whatsapp", entradas.get(0).origen());
        assertEquals("app", entradas.get(1).origen());
    }

    @Test
    void respetaElLimiteSolicitado() {
        ChatLogService logs = new ChatLogService();
        for (int i = 0; i < 10; i++) {
            logs.registrar("app", "entrada", "mensaje " + i, "procesado");
        }
        assertEquals(3, logs.listar(3).size());
    }

    @Test
    void laBitacoraEmpiezaVacia() {
        assertTrue(new ChatLogService().listar(10).isEmpty());
    }
}
