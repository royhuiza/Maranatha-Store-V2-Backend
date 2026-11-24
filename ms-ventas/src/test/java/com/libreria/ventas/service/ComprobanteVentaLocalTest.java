package com.libreria.ventas.service;
import com.libreria.ventas.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
class ComprobanteVentaLocalTest {
    @Test void boletaMixtaYFactura() {
        Venta v=new Venta(); v.setEstado(EstadoVenta.EMITIDA); v.setTipoComprobante(TipoComprobante.BOLETA); v.setSerie("B001"); v.setNumero("00000001"); v.setFecha(LocalDateTime.of(2026,11,24,12,0)); v.setFechaZona("America/Lima"); v.setTotal(new BigDecimal("198.00"));
        agregar(v,"GRAVADO","118.00"); agregar(v,"EXONERADO","50.00"); agregar(v,"INAFECTO","30.00");
        var vista=ComprobanteVentaLocalService.construir(v);
        assertEquals("03",vista.tipoComprobante()); assertEquals(new BigDecimal("18.00"),vista.igv()); assertEquals(vista.igv(),v.getIgv()); assertEquals(new BigDecimal("180.00"),v.getBaseImponible()); assertFalse(vista.validezFiscal()); assertNull(vista.contenidoQr());
        v.setTipoComprobante(TipoComprobante.FACTURA); v.setSerie("F001"); assertThrows(org.springframework.web.server.ResponseStatusException.class,()->ComprobanteVentaLocalService.construir(v)); v.setClienteDocumento("RUC 20123456786"); assertEquals("01",ComprobanteVentaLocalService.construir(v).tipoComprobante());
    }
    private void agregar(Venta v,String tipo,String importe) { VentaDetalle d=new VentaDetalle(); d.setAfectacionIgv(tipo); d.setSubtotal(new BigDecimal(importe)); v.agregarDetalle(d); }
}
