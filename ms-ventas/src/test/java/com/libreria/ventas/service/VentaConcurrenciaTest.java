package com.libreria.ventas.service;

import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/db_ventas_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=${TEST_DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
        VentaFiscalService.class,
        VentaOperacionService.class,
        VentaOperacionInsercionService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VentaConcurrenciaTest {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private VentaFiscalService fiscalService;

    @Autowired
    private VentaOperacionService operacionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void verificarBaseDeDatos() {

        String base = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );

        assertEquals(
                "db_ventas_test",
                base,
                "La prueba solo puede ejecutarse en db_ventas_test"
        );
    }

    private Venta crearVentaPrueba() {

        verificarBaseDeDatos();

        Venta venta = new Venta();

        venta.setSerie("F9B1");
        venta.setNumero("00000001");
        venta.setFecha(LocalDateTime.now());

        venta.setEstado(EstadoVenta.EMITIDA);
        venta.setEstadoSunat(EstadoSunat.PENDIENTE);

        venta.setTipoComprobante(
                TipoComprobante.FACTURA
        );

        venta.setClienteNombre("CLIENTE DE PRUEBA");

        venta.setTotal(new BigDecimal("10.00"));

        venta.setIdempotencyKey(
                "TEST-" + UUID.randomUUID()
        );

        return ventaRepository.saveAndFlush(venta);
    }

    @Test
    void debePermitirUnaSolaReservaFiscal()
            throws Exception {

        Venta venta = crearVentaPrueba();

        Long ventaId = venta.getId();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CyclicBarrier barrera =
                new CyclicBarrier(2);

        Callable<Boolean> solicitud = () -> {

            barrera.await(10, TimeUnit.SECONDS);

            return fiscalService.iniciarEmision(
                    ventaId
            );
        };

        try {

            Future<Boolean> primera =
                    executor.submit(solicitud);

            Future<Boolean> segunda =
                    executor.submit(solicitud);

            boolean resultado1 =
                    primera.get(30, TimeUnit.SECONDS);

            boolean resultado2 =
                    segunda.get(30, TimeUnit.SECONDS);

            int reservas =
                    (resultado1 ? 1 : 0)
                            + (resultado2 ? 1 : 0);

            assertEquals(1, reservas);

            Venta actualizada = ventaRepository
                    .findById(ventaId)
                    .orElseThrow();

            assertEquals(
                    EstadoSunat.EN_PROCESO,
                    actualizada.getEstadoSunat()
            );

            System.out.println(
                    "OK: UNA SOLA RESERVA FISCAL EN POSTGRESQL"
            );

        } finally {

            executor.shutdownNow();

            if (executor.awaitTermination(
                    10, TimeUnit.SECONDS)) {

                ventaRepository.deleteById(ventaId);
            }
        }
    }

    @Test
    void debeImpedirReservaFiscalDeNotaPedido() {

        Venta venta = crearVentaPrueba();

        venta.setTipoComprobante(
                TipoComprobante.NOTA_PEDIDO
        );

        venta.setEstadoSunat(
                EstadoSunat.NO_APLICA
        );

        ventaRepository.saveAndFlush(venta);

        try {

            assertFalse(
                    fiscalService.iniciarEmision(
                            venta.getId()
                    )
            );

            System.out.println(
                    "OK: NOTA DE PEDIDO EXCLUIDA DE SUNAT"
            );

        } finally {

            ventaRepository.deleteById(
                    venta.getId()
            );
        }
    }

    @Test
    void debeReservarUnaSolaOperacionComercial()
            throws Exception {

        verificarBaseDeDatos();

        String clave =
                "CONCURRENCIA-" + UUID.randomUUID();

        String hash = "a".repeat(64);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CyclicBarrier barrera =
                new CyclicBarrier(2);

        Callable<ResultadoReservaVenta> solicitud =
                () -> {

                    barrera.await(10, TimeUnit.SECONDS);

                    return operacionService
                            .reservarConResultado(
                                    clave, hash
                            );
                };

        try {

            Future<ResultadoReservaVenta> primera =
                    executor.submit(solicitud);

            Future<ResultadoReservaVenta> segunda =
                    executor.submit(solicitud);

            ResultadoReservaVenta r1 =
                    primera.get(30, TimeUnit.SECONDS);

            ResultadoReservaVenta r2 =
                    segunda.get(30, TimeUnit.SECONDS);

            assertEquals(
                    r1.operacion().getId(),
                    r2.operacion().getId()
            );

            int nuevas =
                    (r1.nueva() ? 1 : 0)
                            + (r2.nueva() ? 1 : 0);

            assertEquals(1, nuevas);

            System.out.println(
                    "OK: UNA SOLA OPERACION COMERCIAL"
            );

        } finally {

            executor.shutdownNow();

            if (executor.awaitTermination(
                    10, TimeUnit.SECONDS)) {

                jdbcTemplate.update(
                        "DELETE FROM venta_operaciones "
                                + "WHERE idempotency_key = ?",
                        clave
                );
            }
        }
    }
}
