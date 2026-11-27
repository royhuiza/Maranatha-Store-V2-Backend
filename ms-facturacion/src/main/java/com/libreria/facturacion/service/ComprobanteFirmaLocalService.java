
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class ComprobanteFirmaLocalService {

    private final FacturaXmlService facturaXmlService;
    private final FirmaDigitalService firmaDigitalService;
    private final XmlValidationService xmlValidationService;
    private final QrFiscalService qrFiscalService;

    @Value("${sunat.certificado.ruta:}")
    private String certificado;

    @Value("${sunat.certificado.password:}")
    private String password;

    @Value("${sunat.xsd.ruta:}")
    private String xsd;

    @Value("${sunat.certificado.alias:maranatha-beta}")
    private String alias;

    @Value("${facturacion.local.enabled:false}")
    private boolean habilitado;

    public ComprobanteFirmaLocalService(
            FacturaXmlService facturaXmlService,
            FirmaDigitalService firmaDigitalService,
            XmlValidationService xmlValidationService,
            QrFiscalService qrFiscalService
    ) {
        this.facturaXmlService = facturaXmlService;
        this.firmaDigitalService = firmaDigitalService;
        this.xmlValidationService = xmlValidationService;
        this.qrFiscalService = qrFiscalService;
    }

    public ResultadoFirma firmar(
            FacturaPruebaRequest solicitud
    ) {

        if (!habilitado) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La firma local no esta habilitada"
            );
        }

        if (solicitud == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solicitud de firma obligatoria"
            );
        }

        boolean boleta =
                "03".equals(solicitud.tipoComprobante())
                        && "B001".equals(solicitud.serie());

        boolean factura =
                "01".equals(solicitud.tipoComprobante())
                        && "F001".equals(solicitud.serie());

        if ((!boleta && !factura)
                || solicitud.numero() == null
                || !solicitud.numero().matches("[0-9]{8}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comprobante comercial invalido"
            );
        }

        // VALIDAR IDENTIDAD DEL EMISOR DE PRUEBA.

        if (!"20123456786".equals(solicitud.rucEmisor())
                || !"MARANATHA STORE - PRUEBA SIN VALIDEZ FISCAL"
                .equals(solicitud.razonSocialEmisor())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Emisor no autorizado para firma local de prueba"
            );
        }

        // VALIDAR RECEPTOR DE LABORATORIO.

        boolean receptorValido =
                factura
                        ? "6".equals(solicitud.tipoDocumentoCliente())
                        && "20123456786".equals(solicitud.rucCliente())
                        : "1".equals(solicitud.tipoDocumentoCliente())
                        && "00000000".equals(solicitud.rucCliente());

        if (!receptorValido
                || !"CLIENTE DE PRUEBA"
                .equals(solicitud.razonSocialCliente())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Receptor no autorizado para firma local de prueba"
            );
        }

        if (certificado.isBlank()
                || password.isBlank()
                || xsd.isBlank()
                || alias.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configuracion de firma digital incompleta"
            );
        }

        char[] clave = password.toCharArray();

        try (var entrada = Files.newInputStream(
                Path.of(certificado))) {

            // GENERAR XML UBL 2.1.

            String xml = facturaXmlService
                    .generarXml(solicitud);

            // FIRMAR Y VERIFICAR CRIPTOGRAFICAMENTE EL XML.

            String xmlFirmado = firmaDigitalService.firmar(
                    xml,
                    entrada,
                    clave,
                    alias
            );

            // VALIDAR ESTRUCTURA DEL DOCUMENTO.

            xmlValidationService.validarXml(
                    xmlFirmado,
                    Path.of(xsd)
            );

            // EXTRAER INFORMACION DEL QR.

            QrFiscalService.DatosQr datosQr =
                    qrFiscalService.extraer(xmlFirmado);

            return new ResultadoFirma(
                    "LOCAL",
                    "FIRMADO_LOCAL",
                    false,
                    solicitud.serie(),
                    solicitud.numero(),
                    datosQr.igv(),
                    datosQr.total(),
                    datosQr.hash(),
                    datosQr.contenidoQr(),
                    xmlFirmado
            );

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo firmar o verificar el comprobante local",
                    ex
            );

        } finally {

            Arrays.fill(clave, '\0');
        }
    }

    public record ResultadoFirma(
            String ambiente,
            String estado,
            boolean validezFiscal,
            String serie,
            String numero,
            String igv,
            String total,
            String hash,
            String contenidoQr,
            String xmlFirmado
    ) {}
}
