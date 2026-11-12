package com.libreria.ventas.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.ventas.client.*;
import com.libreria.ventas.dto.*;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PaymentSelectionTest {
 @Test void storesMethodWithoutClaimingCollection(){
  var repo=mock(VentaRepository.class);var products=mock(ProductoClient.class);
  var p=new ProductoDTO();p.setId(1L);p.setNombre("Producto de prueba");p.setPrecio(new BigDecimal("11.80"));p.setStock(20);p.setActivo(true);
  when(products.obtenerProducto(1L)).thenReturn(p);
  when(repo.saveAndFlush(any(Venta.class))).thenAnswer(i->{Venta v=i.getArgument(0);v.setId(44L);return v;});
  var request=new CrearVentaRequest();request.setTipoComprobante(TipoComprobante.BOLETA);request.setMetodoPago(MetodoPago.YAPE);
  var detail=new DetalleVentaRequest();detail.setProductoId(1L);detail.setCantidad(2);request.setDetalles(List.of(detail));
  var venta=new VentaServiceImpl(repo,mock(ClienteClient.class),products,new ObjectMapper(),null).crearVenta(request);
  assertEquals(MetodoPago.YAPE,venta.getMetodoPago());assertEquals("PENDIENTE",venta.getEstadoPago());
  assertEquals(new BigDecimal("23.60"),venta.getTotal());assertEquals(new BigDecimal("3.60"),venta.getIgv());
  verify(products).aplicarStockVenta(any(java.util.UUID.class), eq(java.util.Map.of(1L, 2)), eq(false));
 }
 @Test void rejectsUnsupportedMethod(){assertThrows(com.fasterxml.jackson.databind.exc.InvalidFormatException.class,()->new ObjectMapper().readValue("{\"metodoPago\":\"BANCO_FALSO\"}",CrearVentaRequest.class));}
}
