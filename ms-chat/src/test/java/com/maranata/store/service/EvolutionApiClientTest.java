package com.maranata.store.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EvolutionApiClientTest {

    private RestTemplate http;
    private MockRestServiceServer server;
    private EvolutionApiClient client;

    private void setup(String apiKey) {
        http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        client = new EvolutionApiClient(http, "http://maranatha-whatsapp:8080", apiKey, "maranatha-store");
    }

    @Test
    void devuelveElBase64DelQr() {
        setup("clave");
        server.expect(requestTo("http://maranatha-whatsapp:8080/instance/connect/maranatha-store"))
                .andExpect(header("apikey", "clave"))
                .andRespond(withSuccess("{\"base64\":\"data:image/png;base64,AAA\",\"status\":\"connecting\"}", MediaType.APPLICATION_JSON));

        EvolutionApiClient.Qr qr = client.qr();

        assertEquals("data:image/png;base64,AAA", qr.base64());
        assertEquals("connecting", qr.estado());
        assertEquals("maranatha-store", qr.instancia());
        server.verify();
    }

    @Test
    void aceptaElCampoCodeComoAlternativa() {
        setup("clave");
        server.expect(requestTo("http://maranatha-whatsapp:8080/instance/connect/maranatha-store"))
                .andRespond(withSuccess("{\"code\":\"data:image/png;base64,BBB\"}", MediaType.APPLICATION_JSON));

        assertEquals("data:image/png;base64,BBB", client.qr().base64());
    }

    @Test
    void sinClaveNoConsultaLaApi() {
        setup("");
        assertThrows(ResponseStatusException.class, () -> client.qr());
        server.verify();
    }

    @Test
    void instanceInexistenteDevuelve404() {
        setup("clave");
        server.expect(requestTo("http://maranatha-whatsapp:8080/instance/connect/maranatha-store"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> client.qr());
        assertEquals(404, ex.getStatusCode().value());
    }
}
