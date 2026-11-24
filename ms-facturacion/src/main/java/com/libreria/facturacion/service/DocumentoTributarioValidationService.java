
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import org.springframework.stereotype.Service;

@Service
public class DocumentoTributarioValidationService {



    public void validarFactura(FacturaPruebaRequest factura) {

        if (factura == null) {
            throw new IllegalArgumentException(
                    "Los datos del comprobante son obligatorios"
            );
        }

        String tipo = factura.tipoComprobante();

        if (!"01".equals(tipo) && !"03".equals(tipo)) {
            throw new IllegalArgumentException(
                    "Solo se admiten facturas (01) y boletas (03)"
            );
        }

        if (!esRucValido(factura.rucEmisor())) {
            throw new IllegalArgumentException(
                    "El RUC del emisor no es válido"
            );
        }

        if (factura.fechaEmision() == null) {
            throw new IllegalArgumentException(
                    "La fecha de emisión es obligatoria"
            );
        }

        if (factura.serie() == null ||
                factura.numero() == null ||
                factura.serie().isBlank() ||
                factura.numero().isBlank()) {

            throw new IllegalArgumentException(
                    "La serie y número son obligatorios"
            );
        }

        if ("01".equals(tipo)) {

            if (!factura.serie().startsWith("F")) {
                throw new IllegalArgumentException(
                        "La serie de factura debe comenzar con F"
                );
            }

            if (!"6".equals(factura.tipoDocumentoCliente())) {
                throw new IllegalArgumentException(
                        "La factura requiere cliente con RUC"
                );
            }

            if (!esRucValido(factura.rucCliente())) {
                throw new IllegalArgumentException(
                        "El RUC del cliente no es válido"
                );
            }

        } else {

            if (!factura.serie().startsWith("B")) {
                throw new IllegalArgumentException(
                        "La serie de boleta debe comenzar con B"
                );
            }

            String tipoDocumento =
                    factura.tipoDocumentoCliente();

            if (tipoDocumento == null ||
                    !tipoDocumento.matches("0|1|4|6|7")) {

                throw new IllegalArgumentException(
                        "Tipo de documento del cliente no admitido"
                );
            }

            if ("1".equals(tipoDocumento) &&
                    (factura.rucCliente() == null ||
                            !factura.rucCliente().matches("\\d{8}"))) {

                throw new IllegalArgumentException(
                        "El DNI debe contener 8 dígitos"
                );
            }

            if ("6".equals(tipoDocumento) &&
                    !esRucValido(factura.rucCliente())) {

                throw new IllegalArgumentException(
                        "El RUC del cliente no es válido"
                );
            }
        }

        if (factura.detalles() == null ||
                factura.detalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "El comprobante debe contener productos"
            );
        }
    }




    private boolean esRucValido(String ruc) {

        if (ruc == null || !ruc.matches("\\d{11}")) {
            return false;
        }

        String prefijo = ruc.substring(0, 2);

        if (!prefijo.equals("10")
                && !prefijo.equals("15")
                && !prefijo.equals("17")
                && !prefijo.equals("20")) {
            return false;
        }

        int[] factores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

        int suma = 0;

        for (int i = 0; i < factores.length; i++) {
            int digito = Character.digit(ruc.charAt(i), 10);
            suma += digito * factores[i];
        }

        int resto = suma % 11;
        int verificador = 11 - resto;

        if (verificador == 10) {
            verificador = 0;
        } else if (verificador == 11) {
            verificador = 1;
        }

        int ultimoDigito = Character.digit(ruc.charAt(10), 10);

        return verificador == ultimoDigito;
    }

}
