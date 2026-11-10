package com.libreria.ventas.service;

import com.libreria.ventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** T67/T69: validaciones del historial filtrado y paginado. */
class HistorialVentaServiceTest {

    private final VentaRepository repo = mock(VentaRepository.class);
    private final HistorialVentaService service = new HistorialVentaService(repo, null);

    @Test
    void rechazaRangoDeFechasInvertido() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.consultar("2026-11-05", "2026-11-01", null, 0, 10));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repo);
    }

    @Test
    void rechazaFechaConFormatoInvalido() {
        assertThrows(ResponseStatusException.class, () -> service.consultar("05/11/2026", null, null, 0, 10));
    }

    @Test
    void rechazaPaginaYTamanoFueraDeRango() {
        assertThrows(ResponseStatusException.class, () -> service.consultar(null, null, null, -1, 10));
        assertThrows(ResponseStatusException.class, () -> service.consultar(null, null, null, 0, 0));
        assertThrows(ResponseStatusException.class, () -> service.consultar(null, null, null, 0, 101));
    }

    @Test
    void convierteHoraDePeruAUtc() {
        assertEquals(java.time.LocalDateTime.of(2026, 11, 2, 5, 0),
                HistorialVentaService.aUtc(java.time.LocalDateTime.of(2026, 11, 2, 0, 0)));
    }

    @Test
    void normalizaElNumeroDeComprobante() {
        assertEquals("00000012", HistorialVentaService.correlativo("12"));
        assertEquals("00000012", HistorialVentaService.correlativo("00000012"));
        assertEquals("B001-00000012", HistorialVentaService.textoNumero(" b001 - 00000012 "));
        assertEquals("B001", HistorialVentaService.textoNumero("B00%1_"));
    }
}
