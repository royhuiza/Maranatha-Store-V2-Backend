package com.libreria.ventas.service;

import com.libreria.ventas.dto.CrearVentaRequest;
import com.libreria.ventas.dto.DetalleVentaRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VentaRequestHashService {

    public String calcular(CrearVentaRequest request) {
        Map<Long, Integer> productos = new TreeMap<>();

        for (DetalleVentaRequest detalle : request.getDetalles()) {
            productos.merge(
                    detalle.getProductoId(),
                    detalle.getCantidad(),
                    Math::addExact
            );
        }

        StringBuilder contenido = new StringBuilder();

        contenido.append("v1|");
        contenido.append(request.getTipoComprobante().name()).append("|");
        contenido.append(request.getClienteId() == null
                ? "SIN_CLIENTE"
                : request.getClienteId());
        if (request.getClienteId() == null &&
                request.getNumeroDocumento() != null) {

            agregarCampo(contenido, request.getTipoDocumento());
            agregarCampo(contenido, request.getNumeroDocumento());
            agregarCampo(contenido, request.getClienteNombre());
        }
        if (request.getMetodoPago() != null || (request.getDocumento() != null && !request.getDocumento().isBlank())) {
            contenido.append("|datos-adicionales");
            agregarCampo(contenido, request.getMetodoPago() == null ? null : request.getMetodoPago().name());
            agregarCampo(contenido, request.getDocumento());
        }
        productos.forEach((productoId, cantidad) ->
                contenido.append("|")
                        .append(productoId)
                        .append(":")
                        .append(cantidad)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    contenido.toString().getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "No se pudo calcular la huella de la compra", ex
            );
        }
    }
    private void agregarCampo(StringBuilder contenido, String valor) {
        String texto = valor == null ? "" : valor.trim();

        contenido.append("|")
                .append(texto.length())
                .append(":")
                .append(texto);
    }
}
