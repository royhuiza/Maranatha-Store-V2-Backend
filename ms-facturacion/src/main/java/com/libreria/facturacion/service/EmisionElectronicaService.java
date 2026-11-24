package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class EmisionElectronicaService {

    private final FacturaXmlService facturaXmlService;
    private final FirmaDigitalService firmaDigitalService;
    private final XmlValidationService xmlValidationService;
    private final SunatSoapClient sunatSoapClient;
    private final CdrService cdrService;
    private final ArchivoFiscalService archivoFiscalService;

    public EmisionElectronicaService(
            FacturaXmlService facturaXmlService,
            FirmaDigitalService firmaDigitalService,
            XmlValidationService xmlValidationService,
            SunatSoapClient sunatSoapClient,
            CdrService cdrService,
            ArchivoFiscalService archivoFiscalService
    ) {

        this.facturaXmlService = facturaXmlService;
        this.firmaDigitalService = firmaDigitalService;
        this.xmlValidationService = xmlValidationService;
        this.sunatSoapClient = sunatSoapClient;
        this.cdrService = cdrService;
        this.archivoFiscalService = archivoFiscalService;
    }

    public CdrService.ResultadoCdr emitirBeta(
            FacturaPruebaRequest factura,
            Path rutaCertificado,
            char[] password,
            Path rutaXsd
    ) throws Exception {

        try {

            // 1. Generar XML
            String xml =
                    facturaXmlService.generarXml(factura);

            // 2. Firmar comprobante
            String xmlFirmado;

            try (InputStream certificado =
                         Files.newInputStream(rutaCertificado)) {

                xmlFirmado = firmaDigitalService.firmar(
                        xml,
                        certificado,
                        password,
                        "maranatha-beta"
                );
            }

            // 3. Validar estructura
            xmlValidationService.validarXml(
                    xmlFirmado,
                    rutaXsd
            );

            // 4. Identificador del comprobante
            String nombreArchivo =
                    factura.rucEmisor() + "-"
                            + factura.tipoComprobante() + "-"
                            + factura.serie() + "-"
                            + factura.numero();

            // 5. Reservar almacenamiento
            Path carpeta =
                    archivoFiscalService.reservarComprobante(
                            nombreArchivo
                    );

            // 6. Guardar XML antes de enviarlo
            archivoFiscalService.guardarXmlFirmado(
                    carpeta,
                    xmlFirmado
            );

            // 7. Enviar a SUNAT Beta
            String respuesta =
                    sunatSoapClient.enviarFactura(
                            factura.rucEmisor(),
                            nombreArchivo,
                            xmlFirmado
                    );

            // 8. Guardar respuesta original
            archivoFiscalService.guardarRespuestaSoap(
                    carpeta,
                    respuesta
            );

            // 9. Procesar CDR
            CdrService.ResultadoCdr resultado =
                    cdrService.procesar(respuesta);

            // 10. Guardar constancia de recepcion
            if (resultado.xmlCdr() != null) {

                archivoFiscalService.guardarCdr(
                        carpeta,
                        resultado.xmlCdr()
                );
            }

            return resultado;

        } finally {

            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
