package com.maranata.store.service;

import com.maranata.store.dto.ChatRequestDTO;
import com.maranata.store.dto.ChatResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class N8nService {

    private static final Logger log = LoggerFactory.getLogger(N8nService.class);

    @Value("${services.n8n.url:http://maranatha-n8n:5678/webhook/maranatha-chat}")
    private String n8nWebhookUrl;

    /** Numero opcional de destino para reenviar mensajes a WhatsApp (formato internacional). */
    @Value("${integrations.evolution-api.destino:}")
    private String whatsappDestino;

    private final RestTemplate restTemplate;
    private final EvolutionApiClient evolution;

    public N8nService(RestTemplateBuilder restTemplateBuilder, EvolutionApiClient evolution) {
        this.restTemplate = restTemplateBuilder.build();
        this.evolution = evolution;
    }

    public ChatResponseDTO sendMessage(ChatRequestDTO request) {
        try {
            ResponseEntity<ChatResponseDTO> response = restTemplate.postForEntity(
                    n8nWebhookUrl,
                    request,
                    ChatResponseDTO.class
            );
            return response.getBody();
        } catch (RestClientException ex) {
            log.error("Error al conectar con n8n: ", ex);
            return new ChatResponseDTO("Lo siento, el servicio de chat no está disponible en este momento. Intenta nuevamente más tarde.");
        }
    }

    public void forwardToN8nAndWhatsApp(ChatRequestDTO request) {
        try {
            restTemplate.postForEntity(
                    n8nWebhookUrl,
                    request,
                    ChatResponseDTO.class
            );
            log.info("Mensaje reenviado a n8n: {}", request.getMessage());
        } catch (RestClientException ex) {
            log.error("Error al reenviar mensaje a n8n: ", ex);
        }

        if (whatsappDestino == null || whatsappDestino.isBlank()) {
            log.info("Reenvio a WhatsApp omitido: no hay numero destino configurado");
            return;
        }
        try {
            evolution.enviarTexto(whatsappDestino, request.getMessage());
            log.info("Mensaje reenviado a WhatsApp: {}", request.getMessage());
        } catch (RuntimeException ex) {
            log.error("Error al reenviar mensaje a WhatsApp: ", ex);
        }
    }
}
