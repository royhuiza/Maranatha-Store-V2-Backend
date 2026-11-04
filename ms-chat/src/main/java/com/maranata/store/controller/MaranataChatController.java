package com.maranata.store.controller;

import com.maranata.store.dto.ChatRequestDTO;
import com.maranata.store.dto.ChatResponseDTO;
import com.maranata.store.dto.SendTextRequestDTO;
import com.maranata.store.dto.WhatsappWebhookPayloadDTO;
import com.maranata.store.service.N8nService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = "*")
public class MaranataChatController {

    private final N8nService n8nService;
    private final RestTemplate restTemplate;

    public MaranataChatController(N8nService n8nService, RestTemplate restTemplate) {
        this.n8nService = n8nService;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = n8nService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/whatsapp-webhook")
    public ResponseEntity<Void> whatsappWebhook(@RequestBody WhatsappWebhookPayloadDTO payload) {
        String remoteJid = null;
        String conversation = null;

        if (payload != null && payload.getData() != null) {
            if (payload.getData().getKey() != null) {
                remoteJid = payload.getData().getKey().getRemoteJid();
            }
            if (payload.getData().getMessage() != null) {
                conversation = payload.getData().getMessage().getConversation();
            }
        }

        if (remoteJid == null || conversation == null) {
            return ResponseEntity.badRequest().build();
        }

        ChatRequestDTO chatRequest = new ChatRequestDTO(conversation);
        ChatResponseDTO n8nResponse = n8nService.sendMessage(chatRequest);
        String reply = n8nResponse != null ? n8nResponse.getReply() : "Lo siento, no pude procesar tu mensaje.";

        String evolutionUrl = "http://maranatha-whatsapp:8080/message/sendText/maranatha-store";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", "maranatha_secret_key_2026");

        SendTextRequestDTO sendTextRequest = new SendTextRequestDTO(remoteJid, reply);
        HttpEntity<SendTextRequestDTO> entity = new HttpEntity<>(sendTextRequest, headers);

        try {
            restTemplate.postForEntity(evolutionUrl, entity, String.class);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/whatsapp/qr")
    public ResponseEntity<String> getWhatsAppQr() {
        String instanceName = "maranatha-store";
        String evolutionUrl = "http://maranatha-whatsapp:8080/instance/connect/" + instanceName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", "maranatha_secret_key_2026");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    evolutionUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"No se pudo obtener el código QR de WhatsApp\"}");
        }
    }
}
