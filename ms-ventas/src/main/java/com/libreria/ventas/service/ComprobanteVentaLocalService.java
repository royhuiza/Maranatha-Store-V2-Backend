package com.libreria.ventas.service;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class ComprobanteVentaLocalService {
    private final VentaRepository repository;
    public ComprobanteVentaLocalService(VentaRepository repository) { this.repository = repository; }
    @Transactional(readOnly=true)
    public Vista consultar(Long id) {
        return construir(repository.findOneById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada")));
    }
    public static Vista construir(Venta v) {
        if (v.getEstado()!=EstadoVenta.EMITIDA || (v.getTipoComprobante()!=TipoComprobante.FACTURA && v.getTipoComprobante()!=TipoComprobante.BOLETA)) throw invalido();
        boolean factura=v.getTipoComprobante()==TipoComprobante.FACTURA;
        String documento=Objects.toString(v.getClienteDocumento(),"").trim();
        String tipo="0", numero="-";
        if(documento.matches("RUC [0-9]{11}")) { tipo="6"; numero=documento.substring(4); }
        else if(documento.matches("DNI [0-9]{8}")) { tipo="1"; numero=documento.substring(4); }
        else if(!documento.isEmpty()) throw invalido();
        if(factura && !"6".equals(tipo)) throw invalido();
        if(v.getFecha()==null || v.getSerie()==null || !v.getSerie().matches(factura?"F[A-Z0-9]{3}":"B[A-Z0-9]{3}") || v.getNumero()==null || !v.getNumero().matches("[0-9]{1,8}") || v.getDetalles().isEmpty()) throw invalido();
        BigDecimal gravado=BigDecimal.ZERO.setScale(2), exonerado=gravado, inafecto=gravado, igv=gravado;
        for(var d:v.getDetalles()) {
            if(d.getSubtotal()==null || d.getSubtotal().signum()<0 || d.getAfectacionIgv()==null) throw invalido();
            BigDecimal importe=d.getSubtotal();
            switch(d.getAfectacionIgv()) {
                case "GRAVADO" -> { BigDecimal base=importe.divide(new BigDecimal("1.18"),2,RoundingMode.HALF_UP); gravado=gravado.add(base); igv=igv.add(importe.subtract(base)); }
                case "EXONERADO" -> exonerado=exonerado.add(importe);
                case "INAFECTO" -> inafecto=inafecto.add(importe);
                default -> throw invalido();
            }
        }
        BigDecimal total=gravado.add(exonerado).add(inafecto).add(igv);
        if(v.getTotal()==null || total.compareTo(v.getTotal())!=0) throw invalido();
        return new Vista("BORRADOR_LOCAL",false,factura?"01":"03",v.getSerie(),v.getNumero(),v.getFecha().toLocalDate(),tipo,numero,v.getClienteNombre(),gravado,exonerado,inafecto,igv,total,null);
    }
    private static ResponseStatusException invalido() { return new ResponseStatusException(HttpStatus.CONFLICT,"Venta no apta para previsualizacion tributaria"); }
    public record Vista(String ambiente,boolean validezFiscal,String tipoComprobante,String serie,String numero,LocalDate fechaEmision,String tipoDocumentoCliente,String numeroDocumentoCliente,String clienteNombre,BigDecimal gravado,BigDecimal exonerado,BigDecimal inafecto,BigDecimal igv,BigDecimal total,String contenidoQr) {}
}
