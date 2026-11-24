package com.libreria.ventas.client;

import com.libreria.ventas.dto.ProductoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.libreria.ventas.dto.InventarioOperacionDTO;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

@Component
public class ProductoClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public Optional<InventarioOperacionDTO> consultarOperacionInventario(
            String operationKey) {

        if (operationKey == null
                || operationKey.isBlank()
                || operationKey.length() > 150) {
            throw new IllegalArgumentException(
                    "Clave de operación de inventario inválida"
            );
        }

        try {
            InventarioOperacionDTO operacion = restTemplate.getForObject(
                    baseUrl + "/productos/inventario/operaciones/{operationKey}",
                    InventarioOperacionDTO.class,
                    operationKey
            );

            if (operacion == null) {
                throw new IllegalStateException(
                        "El servicio de productos devolvió una respuesta vacía"
                );
            }

            return Optional.of(operacion);

        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    public Optional<InventarioOperacionDTO> consultarReposicionDeDescuento(
            String operationKey) {

        if (operationKey == null
                || operationKey.isBlank()
                || operationKey.length() > 150) {

            throw new IllegalArgumentException(
                    "Clave de descuento inválida"
            );
        }

        try {
            InventarioOperacionDTO reposicion = restTemplate.getForObject(
                    baseUrl
                            + "/productos/inventario/operaciones/{operationKey}/reposicion",
                    InventarioOperacionDTO.class,
                    operationKey
            );

            if (reposicion == null) {
                throw new IllegalStateException(
                        "El servicio de productos devolvió una reposición vacía"
                );
            }

            return Optional.of(reposicion);

        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }


    public ProductoClient(RestTemplate restTemplate, @Value("${services.producto.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ProductoDTO obtenerProducto(Long productoId) {
        return restTemplate.getForObject(baseUrl + "/productos/" + productoId, ProductoDTO.class);
    }

    public ProductoDTO descontarStock(Long productoId, int cantidad) {
        return restTemplate.postForObject(baseUrl + "/productos/" + productoId + "/stock/descontar",
                Map.of("cantidad", cantidad), ProductoDTO.class);
    }

    public void aplicarStockVenta(UUID idOperacion, Map<Long, Integer> cantidades, boolean reversa) {
        String operacion = reversa ? "revertir" : "descontar";
        restTemplate.postForLocation(baseUrl + "/productos/stock/venta/" + idOperacion + "/" + operacion, cantidades);
    }

    public ProductoDTO descontarStockIdempotente(
            Long productoId,
            int cantidad,
            String operationKey) {

        return restTemplate.postForObject(
                baseUrl + "/productos/" + productoId
                        + "/stock/descontar-idempotente",
                Map.of(
                        "cantidad", cantidad,
                        "operationKey", operationKey
                ),
                ProductoDTO.class
        );
    }

    public ProductoDTO reponerStockIdempotente(
            Long productoId,
            int cantidad,
            String operationKey,
            String operacionOrigenKey) {

        return restTemplate.postForObject(
                baseUrl + "/productos/" + productoId
                        + "/stock/reponer-idempotente",
                Map.of(
                        "cantidad", cantidad,
                        "operationKey", operationKey,
                        "operacionOrigenKey", operacionOrigenKey
                ),
                ProductoDTO.class
        );
    }

    public ProductoDTO reponerStock(Long productoId, int cantidad) {
        return restTemplate.postForObject(baseUrl + "/productos/" + productoId + "/stock/reponer",
                Map.of("cantidad", cantidad), ProductoDTO.class);
    }
}
