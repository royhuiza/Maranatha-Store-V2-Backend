package com.maranata.store.client;

import com.maranata.store.dto.ProductoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ProductoClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductoClient(RestTemplate restTemplate,
                          @Value("${services.producto.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public List<ProductoDTO> buscarProductos(String consulta) {
        if (baseUrl.isBlank()) {
            return List.of();
        }
        String url = baseUrl + "/productos?q=" + java.net.URLEncoder.encode(consulta, java.nio.charset.StandardCharsets.UTF_8);
        try {
            @SuppressWarnings("unchecked")
            List<ProductoDTO> productos = (List<ProductoDTO>) restTemplate.getForObject(url, List.class);
            return productos == null ? List.of() : productos;
        } catch (Exception ex) {
            return List.of();
        }
    }
}
