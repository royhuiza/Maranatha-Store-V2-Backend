package com.libreria.ventas.service;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
@Service
public class EstadoComprobanteService {
    private final VentaRepository repository;
    public EstadoComprobanteService(VentaRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public Vista consultar(Long id) { return construir(repository.findOneById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada"))); }
    public static Vista construir(Venta v) {
        EstadoSunat e=v.getEstadoSunat();
        if(v.getTipoComprobante()==TipoComprobante.NOTA_PEDIDO) e=EstadoSunat.NO_APLICA;
        String codigo=e==null?"SIN_REGISTRO":e.name();
        String etiqueta=e==null?"Sin registro fiscal":switch(e) {
            case NO_APLICA -> "No aplica";
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En proceso";
            case ACEPTADO_BETA -> "Aceptado en Beta";
            case OBSERVADO_BETA -> "Observado en Beta";
            case RECHAZADO_BETA -> "Rechazado en Beta";
            case ACEPTADO -> "Aceptado registrado";
            case OBSERVADO -> "Observado registrado";
            case RECHAZADO -> "Rechazado registrado";
            case ERROR_COMUNICACION -> "Error de comunicacion";
            case REQUIERE_REVISION -> "Requiere revision";
        };
        String ambiente=codigo.endsWith("_BETA")?"BETA":"NO_CONFIRMADO";
        return new Vista(v.getId(),v.getSerie(),v.getNumero(),codigo,etiqueta,ambiente,false,v.getCodigoRespuestaSunat(),v.getFechaRespuestaSunat(),v.getEstado());
    }
    public record Vista(Long ventaId,String serie,String numero,String estadoSunat,String etiqueta,String ambiente,boolean validezFiscalConfirmada,String codigoRespuestaSunat,LocalDateTime fechaRespuestaSunat,EstadoVenta estadoVenta) {}
}
