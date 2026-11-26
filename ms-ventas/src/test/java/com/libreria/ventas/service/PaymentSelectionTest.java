package com.libreria.ventas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.libreria.ventas.client.*;
import com.libreria.ventas.dto.*;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentSelectionTest {

 private static final String EVENTO_YAPE = "b".repeat(64);

 private static final String CLAVE_YAPE =
         "YAPE-" + EVENTO_YAPE;

 private static final String CLAVE_EFECTIVO =
         "TEST-PAGO-1";

 private static final String SQL_YAPE =
         "SELECT monto FROM yape_evento WHERE id=? AND fecha >= ? AND fecha <= ?";

 private VentaRepository repo;
 private ProductoClient products;
 private VentaOperacionService operaciones;
 private VentaRequestHashService hashes;
 private VentaPersistenciaService persistencia;
 private VentaOperacion operacion;
 private JdbcTemplate yapeDb;
 private VentaServiceImpl service;

 @BeforeEach
 void configurar() {

  repo = mock(VentaRepository.class);
  products = mock(ProductoClient.class);
  operaciones = mock(VentaOperacionService.class);
  hashes = mock(VentaRequestHashService.class);
  persistencia = mock(VentaPersistenciaService.class);
  operacion = mock(VentaOperacion.class);
  yapeDb = mock(JdbcTemplate.class);

  service = new VentaServiceImpl(
          repo,
          mock(ClienteClient.class),
          products,
          new ObjectMapper(),
          operaciones,
          hashes,
          persistencia
  );

  ReflectionTestUtils.setField(
          service,
          "yapeDb",
          yapeDb
  );
 }

 private void prepararVenta(String clave) {

  var producto = new ProductoDTO();

  producto.setId(1L);
  producto.setNombre("Producto de prueba");
  producto.setPrecio(new BigDecimal("11.80"));
  producto.setStock(20);
  producto.setActivo(true);
  producto.setAfectacionIgv("GRAVADO");

  when(products.obtenerProducto(1L))
          .thenReturn(producto);

  when(operacion.getId())
          .thenReturn(9L);

  when(hashes.calcular(any(CrearVentaRequest.class)))
          .thenReturn("a".repeat(64));

  when(operaciones.reservarConResultado(
          eq(clave),
          anyString()
  )).thenReturn(
          new ResultadoReservaVenta(operacion, true)
  );

  when(persistencia.guardarVenta(
          any(Venta.class),
          eq(clave)
  )).thenAnswer(i -> {

   Venta venta = i.getArgument(0);

   venta.setId(44L);
   venta.setIdempotencyKey(i.getArgument(1));

   return venta;
  });
 }

 private CrearVentaRequest crearRequest(
         MetodoPago metodo,
         String clave
 ) {

  var request = new CrearVentaRequest();

  request.setTipoComprobante(TipoComprobante.BOLETA);
  request.setMetodoPago(metodo);
  request.setIdempotencyKey(clave);

  var detalle = new DetalleVentaRequest();

  detalle.setProductoId(1L);
  detalle.setCantidad(2);

  request.setDetalles(List.of(detalle));

  return request;
 }

 private void simularPagoYape(List<BigDecimal> montos) {

  when(yapeDb.queryForList(
          eq(SQL_YAPE),
          eq(BigDecimal.class),
          eq(EVENTO_YAPE),
          anyLong(),
          anyLong()
  )).thenReturn(montos);
 }

 @Test
 void storesCashMethodWithoutClaimingCollection() {

  prepararVenta(CLAVE_EFECTIVO);

  var request = crearRequest(
          MetodoPago.EFECTIVO,
          CLAVE_EFECTIVO
  );

  var venta = service.crearVenta(request);

  assertEquals(
          MetodoPago.EFECTIVO,
          venta.getMetodoPago()
  );

  assertEquals(
          "PENDIENTE",
          venta.getEstadoPago()
  );

  assertEquals(
          new BigDecimal("23.60"),
          venta.getTotal()
  );

  assertEquals(
          new BigDecimal("3.60"),
          venta.getIgv()
  );

  verify(products)
          .descontarStockIdempotente(
                  1L, 2, "VENTA-9-1"
          );

  verify(operaciones).completar(9L, 44L);

  verifyNoInteractions(yapeDb);
 }

 @Test
 void storesYapeWithDetectedPayment() {

  prepararVenta(CLAVE_YAPE);

  simularPagoYape(
          List.of(new BigDecimal("23.60"))
  );

  var request = crearRequest(
          MetodoPago.YAPE,
          CLAVE_YAPE
  );

  var venta = service.crearVenta(request);

  assertEquals(
          MetodoPago.YAPE,
          venta.getMetodoPago()
  );

  assertEquals(
          "DETECTADO",
          venta.getEstadoPago()
  );

  assertEquals(
          new BigDecimal("23.60"),
          venta.getTotal()
  );

  assertEquals(
          new BigDecimal("3.60"),
          venta.getIgv()
  );

  var orden = inOrder(
          yapeDb,
          products,
          persistencia,
          operaciones
  );

  orden.verify(yapeDb).queryForList(
          eq(SQL_YAPE),
          eq(BigDecimal.class),
          eq(EVENTO_YAPE),
          anyLong(),
          anyLong()
  );

  orden.verify(products)
          .descontarStockIdempotente(
                  1L, 2, "VENTA-9-1"
          );

  orden.verify(persistencia)
          .guardarVenta(
                  any(Venta.class),
                  eq(CLAVE_YAPE)
          );

  orden.verify(operaciones)
          .completar(9L, 44L);
 }

 @Test
 void rejectsInvalidYapePaymentKey() {

  var request = crearRequest(
          MetodoPago.YAPE,
          CLAVE_EFECTIVO
  );

  var error = assertThrows(
          ResponseStatusException.class,
          () -> service.crearVenta(request)
  );

  assertEquals(
          HttpStatus.BAD_REQUEST,
          error.getStatusCode()
  );

  assertEquals(
          "Clave de pago Yape inv\u00e1lida",
          error.getReason()
  );

  verifyNoInteractions(persistencia, yapeDb);

  verify(products, never())
          .descontarStockIdempotente(
                  anyLong(),
                  anyInt(),
                  anyString()
          );
 }

 @Test
 void rejectsMissingYapePayment() {

  prepararVenta(CLAVE_YAPE);

  simularPagoYape(List.of());

  var request = crearRequest(
          MetodoPago.YAPE,
          CLAVE_YAPE
  );

  var error = assertThrows(
          ResponseStatusException.class,
          () -> service.crearVenta(request)
  );

  assertEquals(
          HttpStatus.CONFLICT,
          error.getStatusCode()
  );

  assertEquals(
          "Pago Yape inexistente, vencido o importe distinto",
          error.getReason()
  );

  verifyNoInteractions(persistencia);

  verify(products, never())
          .descontarStockIdempotente(
                  anyLong(),
                  anyInt(),
                  anyString()
          );

  verify(operaciones, never())
          .completar(anyLong(), anyLong());
 }

 @Test
 void rejectsYapePaymentWithDifferentAmount() {

  prepararVenta(CLAVE_YAPE);

  simularPagoYape(
          List.of(new BigDecimal("20.00"))
  );

  var request = crearRequest(
          MetodoPago.YAPE,
          CLAVE_YAPE
  );

  var error = assertThrows(
          ResponseStatusException.class,
          () -> service.crearVenta(request)
  );

  assertEquals(
          HttpStatus.CONFLICT,
          error.getStatusCode()
  );

  assertEquals(
          "Pago Yape inexistente, vencido o importe distinto",
          error.getReason()
  );

  verifyNoInteractions(persistencia);

  verify(products, never())
          .descontarStockIdempotente(
                  anyLong(),
                  anyInt(),
                  anyString()
          );

  verify(operaciones, never())
          .completar(anyLong(), anyLong());
 }

 @Test
 void rejectsUnsupportedMethod() {

  assertThrows(
          InvalidFormatException.class,
          () -> new ObjectMapper().readValue(
                  "{\"metodoPago\":\"BANCO_FALSO\"}",
                  CrearVentaRequest.class
          )
  );
 }
}