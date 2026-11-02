
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import org.springframework.stereotype.Service;

@Service
public class DocumentoTributarioValidationService {

    public void validarFactura(FacturaPruebaRequest factura) {

        if (factura == null) {
            throw new IllegalArgumentException(
                    "Los datos de la factura son obligatorios"
            );
        }

        if (!"01".equals(factura.tipoComprobante())) {
            throw new IllegalArgumentException(
                    "Este validador solo admite facturas tipo 01"
            );
        }

        if (!esRucValido(factura.rucEmisor())) {
            throw new IllegalArgumentException(
                    "El RUC del emisor no tiene un formato valido"
            );
        }

        if (!"6".equals(factura.tipoDocumentoCliente())) {
            throw new IllegalArgumentException(
                    "La factura requiere un cliente identificado con RUC"
            );
        }

        if (!esRucValido(factura.rucCliente())) {
            throw new IllegalArgumentException(
                    "El RUC del cliente no tiene un formato valido"
            );
        }

        if (factura.fechaEmision() == null) {
            throw new IllegalArgumentException(
                    "La fecha de emision es obligatoria"
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
