package com.libreria.ventas.service;

import com.libreria.ventas.client.FacturacionClient;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/db_ventas_test",
    "spring.datasource.username=postgres",
    "spring.datasource.password=${TEST_DB_PASSWORD}",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VentaFiscalService.class, VentaEmisionBetaService.class, VentaPersistenciaService.class, VentaLaboratorioService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VentaEmisionBetaIntegracionTest {
    @Autowired VentaRepository repository;
    @Autowired VentaFiscalService fiscal;
    @Autowired VentaEmisionBetaService servicio;
    @Autowired JdbcTemplate jdbc;
    @Autowired VentaLaboratorioService laboratorio;
    @MockBean FacturacionClient client;

    private Venta crear(String ruc) {
        assertEquals("db_ventas_test", jdbc.queryForObject("SELECT current_database()", String.class));
        Venta v = new Venta();
        v.setIdempotencyKey("DEMO-FISCAL-" + UUID.randomUUID());
        v.setClienteNombre("EMPRESA FICTICIA DE PRUEBA");
        v.setClienteDocumento("RUC " + ruc);
        v.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
        v.setTipoComprobante(TipoComprobante.FACTURA);
        v.setSerie("F9B1");
        v.setNumero("PENDIENTE");
        v.setEstado(EstadoVenta.EMITIDA);
        v.setEstadoSunat(EstadoSunat.PENDIENTE);
        v.setTotal(new BigDecimal("11.80"));
        VentaDetalle d = new VentaDetalle();
        d.setProductoId(1L); // Referencia sintetica, no consulta ni descuenta stock.
        d.setProductoNombre("Producto de prueba");
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("11.80"));
        d.setSubtotal(new BigDecimal("11.80"));
        d.setAfectacionIgv("GRAVADO");
        v.agregarDetalle(d);
        v = repository.saveAndFlush(v);
        v.setNumero(String.format(java.util.Locale.ROOT, "%08d", v.getId()));
        return repository.saveAndFlush(v);
    }

    @Test
    void flujoLocalAceptadoYDuplicado() {
        String ruc = "00000000000"; // Solo Mockito, nunca se envia a SUNAT.
        Venta v = crear(ruc);
        try {
            when(client.emitirBeta(any())).thenAnswer(inv -> {
                FacturacionClient.SolicitudEmision s = inv.getArgument(0);
                assertEquals(v.getNumero(), s.numero());
                assertEquals("01", s.tipoComprobante());
                return new FacturacionClient.ResultadoEmision("BETA", "ACEPTADA_BETA", "0",
                        "RESPUESTA SIMULADA - NO SUNAT", s.serie(), s.numero());
            });
            assertEquals(EstadoSunat.ACEPTADO_BETA, servicio.emitirFacturaPrueba(v.getId(), ruc).getEstadoSunat());
            assertEquals(EstadoSunat.ACEPTADO_BETA, servicio.emitirFacturaPrueba(v.getId(), ruc).getEstadoSunat());
            verify(client, times(1)).emitirBeta(any());
            System.out.println("OK: FLUJO FISCAL SIMULADO, SIN REENVIO");
        } finally { repository.deleteById(v.getId()); }
    }

    @Test
    void respuestaNoConfirmadaNoSeReenvia() {
        String ruc = "00000000000";
        Venta v = crear(ruc);
        try {
            when(client.emitirBeta(any())).thenThrow(new IllegalStateException("Fallo simulado"));
            assertEquals(EstadoSunat.ERROR_COMUNICACION, servicio.emitirFacturaPrueba(v.getId(), ruc).getEstadoSunat());
            assertEquals(EstadoSunat.ERROR_COMUNICACION, servicio.emitirFacturaPrueba(v.getId(), ruc).getEstadoSunat());
            verify(client, times(1)).emitirBeta(any());
            System.out.println("OK: ERROR CONTROLADO SIN REENVIO");
        } finally { repository.deleteById(v.getId()); }
    }

    @Test
    void registroAutomaticoSinDuplicar() {
        UUID operacion = UUID.randomUUID();
        String clave = "DEMO-FISCAL-" + operacion;
        try {
            when(client.emitirBeta(any())).thenAnswer(inv -> {
                FacturacionClient.SolicitudEmision s = inv.getArgument(0);
                assertEquals("F9B1", s.serie());
                assertEquals("01", s.tipoComprobante());
                return new FacturacionClient.ResultadoEmision("BETA", "ACEPTADA_BETA", "0",
                        "SIMULACION LOCAL - NO SUNAT", s.serie(), s.numero());
            });
            Venta primera = laboratorio.registrarYEmitir(operacion, "00000000000");
            Venta repetida = laboratorio.registrarYEmitir(operacion, "00000000000");
            assertEquals(primera.getId(), repetida.getId());
            assertEquals(EstadoSunat.ACEPTADO_BETA, repetida.getEstadoSunat());
            assertFalse(repetida.isValidadoSunat());
            verify(client, times(1)).emitirBeta(any());
        } finally {
            repository.findByIdempotencyKey(clave).ifPresent(v -> repository.deleteById(v.getId()));
        }
    }

    @Test
    void registroAutomaticoConErrorNoReenvia() {
        UUID operacion = UUID.randomUUID();
        String clave = "DEMO-FISCAL-" + operacion;
        try {
            when(client.emitirBeta(any())).thenThrow(new IllegalStateException("Fallo simulado"));
            Venta primera = laboratorio.registrarYEmitir(operacion, "00000000000");
            Venta repetida = laboratorio.registrarYEmitir(operacion, "00000000000");
            assertEquals(primera.getId(), repetida.getId());
            assertEquals(EstadoSunat.ERROR_COMUNICACION, repetida.getEstadoSunat());
            assertFalse(repetida.isValidadoSunat());
            verify(client, times(1)).emitirBeta(any());
        } finally {
            repository.findByIdempotencyKey(clave).ifPresent(v -> repository.deleteById(v.getId()));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named="MARANATHA_BETA_AUTORIZADO", matches="SI")
    void facturaBetaRealOpcional() {
        String ruc = System.getenv("MARANATHA_BETA_RUC");
        String token = System.getenv("FACTURACION_INTERNAL_TOKEN");
        assertNotNull(ruc, "Configura un RUC de laboratorio autorizado y admitido por Facturacion");
        assertTrue(ruc.matches("[0-9]{11}") && !ruc.equals("00000000000"));
        assertNotNull(token, "Falta el token interno");
        assertFalse(token.isBlank());
        FacturacionClient real = new FacturacionClient(new RestTemplateBuilder(), "http://127.0.0.1:8085");
        ReflectionTestUtils.setField(real, "internalToken", token);
        VentaEmisionBetaService coordinador = new VentaEmisionBetaService(repository, fiscal, real, jdbc);
        Venta v = crear(ruc);
        Venta resultado = coordinador.emitirFacturaPrueba(v.getId(), ruc);
        System.out.println("BETA: " + resultado.getSerie() + "-" + resultado.getNumero()
                + " | " + resultado.getEstadoSunat() + " | codigo=" + resultado.getCodigoRespuestaSunat());
        // Conservar evidencia y estado, incluso si falla. No reintentar automaticamente.
        assertEquals(EstadoSunat.ACEPTADO_BETA, resultado.getEstadoSunat(),
                "Revisar registro de prueba y archivos fiscales antes de otro envio");
    }
}
