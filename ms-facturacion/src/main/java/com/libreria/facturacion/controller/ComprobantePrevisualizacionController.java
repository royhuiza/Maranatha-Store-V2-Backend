package com.libreria.facturacion.controller;
import com.libreria.facturacion.dto.*;
import com.libreria.facturacion.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/facturacion/previsualizacion")
public class ComprobantePrevisualizacionController {
    private final FacturaXmlService xml;
    private final FirmaDigitalService firma;
    private final XmlValidationService validacion;
    private final QrFiscalService qr;
    @Value("${sunat.certificado.ruta:}") private String certificado;
    @Value("${sunat.certificado.password:}") private String password;
    @Value("${sunat.xsd.ruta:}") private String xsd;
    public ComprobantePrevisualizacionController(FacturaXmlService xml,FirmaDigitalService firma,XmlValidationService validacion,QrFiscalService qr) {
        this.xml=xml; this.firma=firma; this.validacion=validacion; this.qr=qr;
    }
    @GetMapping("/{tipo}")
    public QrFiscalService.DatosQr previsualizar(@PathVariable String tipo) {
        if (!Set.of("01","03").contains(tipo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo invalido");
        if (certificado.isBlank() || password.isBlank() || xsd.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configuracion incompleta");
        var solicitud = ejemplo(tipo);
        char[] clave=password.toCharArray();
        try (var entrada=Files.newInputStream(Path.of(certificado))) {
            String firmado=firma.firmar(xml.generarXml(solicitud),entrada,clave,"maranatha-beta");
            validacion.validarXml(firmado,Path.of(xsd));
            return qr.extraer(firmado);
        } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo generar previsualizacion local"); }
        finally { Arrays.fill(clave,'\0'); }
    }
    public static FacturaPruebaRequest ejemplo(String tipo) {
        boolean factura="01".equals(tipo);
        return new FacturaPruebaRequest("20123456786","MARANATHA BETA - PRUEBA",factura?"20123456786":"00000000","CLIENTE DE LABORATORIO",factura?"F9B2":"B9B2","00000001",List.of(new FacturaPruebaRequest.Detalle("Producto de prueba",1,new BigDecimal("11.80"),TipoAfectacionIgv.GRAVADO)),tipo,factura?"6":"1","DIRECCION DE PRUEBA",LocalDate.now(ZoneId.of("America/Lima")));
    }
}
