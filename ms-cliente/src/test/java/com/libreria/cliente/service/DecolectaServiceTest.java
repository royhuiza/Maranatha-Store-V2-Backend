package com.libreria.cliente.service;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Synthetic unit fixtures only; no real identity lookup or network request. */
class DecolectaServiceTest {
 private RestTemplate http;private MockRestServiceServer server;private DecolectaService service;
 @BeforeEach void setup(){http=new RestTemplate();server=MockRestServiceServer.bindTo(http).build();service=new DecolectaService(http,"unit-test-key");}
 private void status(int expected,Runnable action){assertEquals(expected,assertThrows(ResponseStatusException.class,action::run).getStatusCode().value());}
 @Test void invalidDocumentConsumesNoRequest(){status(400,()->service.consultar("DNI","abc"));server.verify();}
 @Test void missingKeyConsumesNoRequest(){status(503,()->new DecolectaService(http,"").consultar("DNI","00000001"));server.verify();}
 @Test void mapsAndCachesDni(){server.expect(requestTo("https://api.decolecta.com/v1/reniec/dni?numero=00000001")).andExpect(header("Authorization","Bearer unit-test-key")).andRespond(withSuccess("{\"document_number\":\"00000001\",\"first_name\":\"PRUEBA\",\"first_last_name\":\"SINTETICA\",\"second_last_name\":\"LOCAL\"}",MediaType.APPLICATION_JSON));var first=service.consultar("DNI","00000001");assertEquals("PRUEBA SINTETICA LOCAL",first.nombre());assertSame(first,service.consultar("DNI","00000001"));server.verify();}
 @Test void mapsRucWithoutInventingAddress(){server.expect(anything()).andRespond(withSuccess("{\"numero_documento\":\"00000000001\",\"razon_social\":\"EMPRESA SINTETICA\"}",MediaType.APPLICATION_JSON));var r=service.consultar("RUC","00000000001");assertEquals("EMPRESA SINTETICA",r.nombre());assertNull(r.direccion());server.verify();}
 @Test void rejectsDifferentIdentity(){server.expect(anything()).andRespond(withSuccess("{\"document_number\":\"00000002\",\"first_name\":\"PRUEBA\",\"first_last_name\":\"LOCAL\"}",MediaType.APPLICATION_JSON));status(502,()->service.consultar("DNI","00000001"));}
 @Test void vendor401DoesNotBecomeSession401(){server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED));status(503,()->service.consultar("DNI","00000001"));}
 @Test void quotaFailureIsReported(){server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));status(429,()->service.consultar("DNI","00000001"));}
 @Test void rejectsEmptyResponse(){server.expect(anything()).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));status(502,()->service.consultar("DNI","00000001"));}
}
