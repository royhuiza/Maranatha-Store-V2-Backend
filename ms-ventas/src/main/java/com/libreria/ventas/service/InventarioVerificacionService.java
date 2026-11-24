package com.libreria.ventas.service;

import com.libreria.ventas.client.ProductoClient;
import com.libreria.ventas.dto.InventarioOperacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class InventarioVerificacionService {

    private final ProductoClient productoClient;

    public InventarioVerificacionService(ProductoClient productoClient) {
        this.productoClient = productoClient;
    }

    public boolean descuentoConfirmado(
            Long productoId,
            int cantidad,
            String operationKey) {

        if (productoId == null || productoId <= 0
                || cantidad <= 0
                || operationKey == null
                || operationKey.isBlank()
                || operationKey.length() > 150) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Datos de verificación de inventario inválidos"
            );
        }

        Optional<InventarioOperacionDTO> resultado =
                productoClient.consultarOperacionInventario(operationKey);

        if (resultado.isEmpty()) {
            return false;
        }

        InventarioOperacionDTO operacion = resultado.get();

        if (!operationKey.equals(operacion.getOperationKey())
                || !productoId.equals(operacion.getProductoId())
                || !Integer.valueOf(cantidad).equals(operacion.getCantidad())
                || !"DESCUENTO".equals(operacion.getTipo())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operación de inventario no coincide con el descuento esperado"
            );
        }

        return true;
    }

    public boolean descuentoCompensado(
            Long productoId,
            int cantidad,
            String operationKey) {

        if (productoId == null || productoId <= 0
                || cantidad <= 0
                || operationKey == null
                || operationKey.isBlank()
                || operationKey.length() > 150) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Datos de verificación de reposición inválidos"
            );
        }

        Optional<InventarioOperacionDTO> resultado =
                productoClient.consultarReposicionDeDescuento(operationKey);

        if (resultado.isEmpty()) {
            return false;
        }

        InventarioOperacionDTO reposicion = resultado.get();

        if (!productoId.equals(reposicion.getProductoId())
                || !Integer.valueOf(cantidad).equals(reposicion.getCantidad())
                || !"REPOSICION".equals(reposicion.getTipo())
                || !operationKey.equals(reposicion.getOperacionOrigenKey())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reposición no coincide con el descuento esperado"
            );
        }

        return true;
    }
}