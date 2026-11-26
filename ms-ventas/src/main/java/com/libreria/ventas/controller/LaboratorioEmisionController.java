package com.libreria.ventas.controller;

import com.libreria.ventas.service.AccesoFiscalService;
import com.libreria.ventas.service.VentaLaboratorioService;
import com.libreria.ventas.model.Venta;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/ventas/laboratorio")
public class LaboratorioEmisionController {
    private final AccesoFiscalService acceso;
    private final ObjectProvider<VentaLaboratorioService> laboratorio;
    private final org.springframework.web.client.RestTemplate http;
    private final boolean aislado;
    public LaboratorioEmisionController(AccesoFiscalService acceso, ObjectProvider<VentaLaboratorioService> laboratorio,
            RestTemplateBuilder builder, @Value("${maranatha.laboratorio.aislado:false}") boolean aislado) {
        this.acceso=acceso; this.laboratorio=laboratorio; this.aislado=aislado;
        this.http=builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(110)).build();
    }
    @PostMapping("/emision-beta/{operacion}")
    public synchronized Resultado emitir(@PathVariable("operacion") UUID operacion,
            @RequestHeader(value="Authorization",required=false) String token,
            @RequestParam(name="a",defaultValue="-1") int a,
            @RequestParam(name="b",defaultValue="-1") int b,
            @RequestParam(name="c",defaultValue="-1") int c,
            @RequestParam(name="items",defaultValue="") String items,
            @RequestParam(name="centimos",defaultValue="0") long centimos) {
        acceso.exigirAdmin(token);
        boolean catalogo = a!=-1 || b!=-1 || c!=-1;
        boolean completo = !items.isEmpty();
        if (completo) {
            if (catalogo) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"No mezclar carritos");
            try { com.libreria.ventas.service.CatalogoBeta.lineas(items,centimos); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Carrito no coincide con copia Beta autorizada o cantidad invalida"); }
        }
        if (catalogo) {
            try { com.libreria.ventas.service.CatalogoBeta.lineas(a,b,c); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Carrito de prueba invalido"); }
        }
        if (aislado) {
            VentaLaboratorioService servicio=laboratorio.getIfAvailable();
            if (servicio==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Laboratorio deshabilitado");
            Venta v=completo ? servicio.registrarCatalogoCompleto(operacion,items,centimos) : catalogo ? servicio.registrarCatalogo(operacion,a,b,c) : servicio.registrarYEmitir(operacion,"20123456786");
            if (!"F9B1".equals(v.getSerie()) || v.isValidadoSunat()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Resultado de laboratorio inconsistente");
            return new Resultado(v.getSerie(),v.getNumero(),v.getEstadoSunat().name(),v.getCodigoRespuestaSunat(),"BETA",false);
        }
        HttpHeaders headers=new HttpHeaders(); headers.set(HttpHeaders.AUTHORIZATION,token);
        try {
            Resultado r=http.exchange("http://maranatha-ventas-lab:8084/ventas/laboratorio/emision-beta/"+operacion+(completo ? "?items="+items+"&centimos="+centimos : catalogo ? "?a="+a+"&b="+b+"&c="+c : ""),
                HttpMethod.POST,new HttpEntity<Void>(headers),Resultado.class).getBody();
            if(r==null || !"BETA".equals(r.ambiente()) || r.validezFiscal() || !"F9B1".equals(r.serie()))
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta de laboratorio inconsistente");
            return r;
        } catch(HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(),"Laboratorio: revise la operacion antes de repetirla");
        } catch(RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Laboratorio no disponible o resultado no confirmado; conserve la operacion");
        }
    }
    public record Resultado(String serie,String numero,String estadoSunat,String codigo,String ambiente,boolean validezFiscal) {}
}
