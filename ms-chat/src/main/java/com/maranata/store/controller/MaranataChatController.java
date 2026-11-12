package com.maranata.store.controller;

import com.maranata.store.client.ProductoClient;
import com.maranata.store.dto.ChatRequestDTO;
import com.maranata.store.dto.ChatResponseDTO;
import com.maranata.store.dto.ProductoDTO;
import com.maranata.store.dto.SendTextRequestDTO;
import com.maranata.store.dto.WhatsappWebhookPayloadDTO;
import com.maranata.store.service.ChatLogService;
import com.maranata.store.service.EvolutionApiClient;
import com.maranata.store.service.N8nService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class MaranataChatController {

    private static final String[] PALABRAS_PRODUCTO = {
            "producto", "productos", "precio", "precios", "stock", "venden", "tienen",
            "disponible", "disponibles", "catalogo", "catálogo", "busco", "quiero",
            "comprar", "venta", "articulo", "artículo", "articulos", "artículos"
    };

    private final N8nService n8nService;
    private final EvolutionApiClient evolution;
    private final ChatLogService chatLogs;
    private final ProductoClient productoClient;

    public MaranataChatController(N8nService n8nService,
                                  EvolutionApiClient evolution, ChatLogService chatLogs,
                                  RestTemplateBuilder restTemplateBuilder,
                                  @Value("${services.producto.url}") String productoUrl) {
        this.n8nService = n8nService;
        this.evolution = evolution;
        this.chatLogs = chatLogs;
        RestTemplate productoRest = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.productoClient = new ProductoClient(productoRest, productoUrl);
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        String mensaje = request == null ? "" : request.getMessage();
        String respuesta = responderSegunBD(mensaje);

        if (respuesta == null) {
            respuesta = responderConFallback(mensaje);
        }

        chatLogs.registrar("app", "entrada", mensaje, "procesado");
        return ResponseEntity.ok(new ChatResponseDTO(respuesta));
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
        String respuesta = responderSegunBD(conversation);

        if (respuesta == null) {
            respuesta = responderConFallback(conversation);
        }

        SendTextRequestDTO sendTextRequest = new SendTextRequestDTO(remoteJid, respuesta);

        try {
            evolution.enviarTexto(remoteJid, respuesta);
            chatLogs.registrar("whatsapp", "salida", respuesta, "enviado");
        } catch (Exception ex) {
            chatLogs.registrar("whatsapp", "salida", respuesta, "error");
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    private String responderSegunBD(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return null;
        }

        String lower = mensaje.toLowerCase();
        boolean consultaProducto = false;
        for (String palabra : PALABRAS_PRODUCTO) {
            if (lower.contains(palabra)) {
                consultaProducto = true;
                break;
            }
        }

        if (!consultaProducto) {
            return null;
        }

        List<ProductoDTO> productos = productoClient.buscarProductos(mensaje);

        if (productos.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *Productos disponibles en Maranatha Store:*\n\n");
        for (ProductoDTO p : productos) {
            sb.append("• *").append(p.getNombre()).append("*\n");
            if (p.getDescripcion() != null && !p.getDescripcion().isBlank()) {
                sb.append("  ").append(p.getDescripcion()).append("\n");
            }
            sb.append("  Precio: S/ ").append(p.getPrecio() == null ? "0.00" : p.getPrecio()).append("\n");
            sb.append("  Stock: ").append(p.getStock() == null ? 0 : p.getStock()).append(" (").append(p.getEstadoStock()).append(")\n\n");
        }
        sb.append("¿Te interesa alguno de estos productos? Dime el nombre y con gusto te ayudo.");

        return sb.toString();
    }

    private String responderConFallback(String mensaje) {
        String lower = mensaje == null ? "" : mensaje.toLowerCase();

        if (lower.contains("hola") || lower.contains("buenos") || lower.contains("buenas")
                || lower.contains("saludo") || lower.contains("inicio")) {
            return "¡Hola! Bienvenido a Maranatha Store. ¿En qué te puedo ayudar hoy?\n\n"
                    + "Puedes consultarme por nuestros productos escribiendo \"productos\" o el nombre de lo que buscas.\n"
                    + "También puedo ayudarte con información sobre ventas y clientes del sistema.";
        }

        if (lower.contains("producto") || lower.contains("precio") || lower.contains("stock")) {
            return "No pude consultar los productos en este momento. Asegúrate de que el servicio de productos esté disponible.\n\n"
                    + "Intenta nuevamente más tarde o escríbeme qué producto estás buscando.";
        }

        return "¡Hola! Soy el asistente de Maranatha Store. ¿En qué te puedo ayudar?\n\n"
                + "Puedes preguntarme por productos, precios o stock del catálogo.";
    }

    @GetMapping("/whatsapp/qr")
    public ResponseEntity<EvolutionApiClient.Qr> getWhatsAppQr() {
        EvolutionApiClient.Qr qr = evolution.qr();
        chatLogs.registrar("sistema", "qr", qr.base64() == null ? "" : "base64", qr.estado());
        return ResponseEntity.ok(qr);
    }

    @GetMapping("/logs")
    public List<ChatLogService.Entry> logs(@RequestParam(required = false, defaultValue = "50") int limite) {
        return chatLogs.listar(limite);
    }
}
