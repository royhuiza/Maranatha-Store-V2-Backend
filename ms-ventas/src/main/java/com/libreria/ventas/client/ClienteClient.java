package com.libreria.ventas.client;

import com.libreria.ventas.dto.ClienteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ClienteClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ClienteClient(RestTemplate restTemplate, @Value("${services.cliente.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ClienteDTO obtenerCliente(Long clienteId) {
        return restTemplate.getForObject(baseUrl + "/clientes/" + clienteId, ClienteDTO.class);
    }
}
