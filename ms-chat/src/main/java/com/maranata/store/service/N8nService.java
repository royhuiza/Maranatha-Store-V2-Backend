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

    @Value("${services.n8n.url:http://maranatha-n8n:5678/webhook/maranata-chat}")
    private String n8nWebhookUrl;

    @Value("${services.whatsapp.url:http://host.docker.internal:8081/message/sendText/maranatha}")
    private String whatsappWebhookUrl;

    private final RestTemplate restTemplate;

    public N8nService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
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

        try {
            restTemplate.postForEntity(
                    whatsappWebhookUrl,
                    request,
                    ChatResponseDTO.class
            );
            log.info("Mensaje reenviado a WhatsApp: {}", request.getMessage());
        } catch (RestClientException ex) {
            log.error("Error al reenviar mensaje a WhatsApp: ", ex);
        }
    }
}
