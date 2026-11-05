package com.libreria.ventas.client;

import com.libreria.ventas.dto.ProductoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class ProductoClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

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

    public ProductoDTO reponerStock(Long productoId, int cantidad) {
        return restTemplate.postForObject(baseUrl + "/productos/" + productoId + "/stock/reponer",
                Map.of("cantidad", cantidad), ProductoDTO.class);
    }
}
