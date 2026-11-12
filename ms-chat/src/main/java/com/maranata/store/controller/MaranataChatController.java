package com.maranata.store.controller;

import com.maranata.store.dto.ChatRequestDTO;
import com.maranata.store.dto.ChatResponseDTO;
import com.maranata.store.dto.SendTextRequestDTO;
import com.maranata.store.dto.WhatsappWebhookPayloadDTO;
import com.maranata.store.service.ChatLogService;
import com.maranata.store.service.EvolutionApiClient;
import com.maranata.store.service.N8nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints del chatbot.
 *
 * Sin @CrossOrigin: el API Gateway ya aplica CORS global para [/**] y responde
 * con el origen concreto. Si este controller anade su propia cabecera
 * Access-Control-Allow-Origin, la respuesta llega al navegador con dos valores
 * ("http://localhost:4200,*") y el navegador rechaza la peticion.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class MaranataChatController {

    private final N8nService n8nService;
    private final EvolutionApiClient evolution;
    private final ChatLogService chatLogs;

    public MaranataChatController(N8nService n8nService,
                                  EvolutionApiClient evolution, ChatLogService chatLogs) {
        this.n8nService = n8nService;
        this.evolution = evolution;
        this.chatLogs = chatLogs;
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = n8nService.sendMessage(request);
        chatLogs.registrar("app", "entrada", request == null ? null : request.getMessage(), "procesado");
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
            chatLogs.registrar("whatsapp", "entrada", conversation, "rechazado");
            return ResponseEntity.badRequest().build();
        }

        ChatRequestDTO chatRequest = new ChatRequestDTO(conversation);
        ChatResponseDTO n8nResponse = n8nService.sendMessage(chatRequest);
        String reply = n8nResponse != null && n8nResponse.getReply() != null
                ? n8nResponse.getReply()
                : "Lo siento, no pude procesar tu mensaje.";

        SendTextRequestDTO sendTextRequest = new SendTextRequestDTO(remoteJid, reply);

        try {
            evolution.enviarTexto(remoteJid, reply);
            chatLogs.registrar("whatsapp", "salida", reply, "enviado");
        } catch (Exception ex) {
            chatLogs.registrar("whatsapp", "salida", reply, "error");
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Codigo QR de WhatsApp en Base64, consultado a Evolution API.
     */
    @GetMapping("/whatsapp/qr")
    public ResponseEntity<EvolutionApiClient.Qr> getWhatsAppQr() {
        EvolutionApiClient.Qr qr = evolution.qr();
        chatLogs.registrar("sistema", "qr", qr.base64() == null ? "" : "base64", qr.estado());
        return ResponseEntity.ok(qr);
    }

    /**
     * Bitacora reciente de la actividad del chatbot.
     */
    @GetMapping("/logs")
    public List<ChatLogService.Entry> logs(@RequestParam(required = false, defaultValue = "50") int limite) {
        return chatLogs.listar(limite);
    }
}
