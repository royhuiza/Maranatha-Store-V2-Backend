package com.libreria.ventas.service;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Protocol fixtures only; these are not official sandbox transactions. */
class CulqiSandboxClientTest {
 private RestTemplate http;private MockRestServiceServer server;
 @BeforeEach void setup(){http=new RestTemplate();server=MockRestServiceServer.bindTo(http).build();}
 @Test void refusesProductionSecret(){assertThrows(ResponseStatusException.class,()->new CulqiSandboxClient(http,"sk_live_not-a-real-key").crearCargoPrueba(new BigDecimal("10"),"tkn_test_fixture","test@example.invalid"));server.verify();}
 @Test void refusesProductionToken(){assertThrows(ResponseStatusException.class,()->new CulqiSandboxClient(http,"sk_test_fixture").crearCargoPrueba(new BigDecimal("10"),"tkn_live_fixture","test@example.invalid"));server.verify();}
 @Test void convertsAmountAndKeepsSandboxState(){server.expect(requestTo("https://api.culqi.com/v2/charges")).andExpect(content().json("{\"amount\":1050,\"currency_code\":\"PEN\",\"email\":\"test@example.invalid\",\"source_id\":\"tkn_test_fixture\"}")).andRespond(withSuccess("{\"id\":\"chr_test_fixture\",\"object\":\"charge\",\"amount\":1050,\"currency\":\"PEN\",\"response_code\":\"venta_exitosa\"}",MediaType.APPLICATION_JSON));var r=new CulqiSandboxClient(http,"sk_test_fixture").crearCargoPrueba(new BigDecimal("10.50"),"tkn_test_fixture","test@example.invalid");assertTrue(r.sandbox());assertEquals("APROBADO_EN_SANDBOX",r.estado());server.verify();}
 @Test void rejectsMismatchedAmount(){server.expect(anything()).andRespond(withSuccess("{\"id\":\"chr_test_fixture\",\"object\":\"charge\",\"amount\":999,\"currency\":\"PEN\",\"response_code\":\"venta_exitosa\"}",MediaType.APPLICATION_JSON));assertThrows(ResponseStatusException.class,()->new CulqiSandboxClient(http,"sk_test_fixture").crearCargoPrueba(new BigDecimal("10"),"tkn_test_fixture","test@example.invalid"));}
 @Test void doesNotRetryUnknownResult(){server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));assertThrows(ResponseStatusException.class,()->new CulqiSandboxClient(http,"sk_test_fixture").crearCargoPrueba(new BigDecimal("10"),"tkn_test_fixture","test@example.invalid"));server.verify();}
}
