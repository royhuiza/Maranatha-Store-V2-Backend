
package com.libreria.cliente.service;

import com.libreria.cliente.model.Cliente;
import com.libreria.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteServiceImplTest {

    private ClienteRepository repository;
    private ClienteServiceImpl service;

    @BeforeEach
    void configurar() {
        repository = mock(ClienteRepository.class);
        service = new ClienteServiceImpl(repository);
    }

    private Cliente crearCliente(String ruc) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Empresa de prueba");
        cliente.setTipoDocumento("RUC");
        cliente.setNumeroDocumento(ruc);
        return cliente;
    }

    @Test
    void debeRechazarRucConDigitoVerificadorIncorrecto() {
        Cliente cliente = crearCliente("12345678901");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.crearCliente(cliente)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(
                "El RUC tiene un digito verificador incorrecto",
                error.getReason()
        );

        verify(repository, never()).save(any(Cliente.class));
    }

    @Test
    void debeRechazarRucConMenosDeOnceDigitos() {
        Cliente cliente = crearCliente("2012345678");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.crearCliente(cliente)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(
                "El RUC debe tener exactamente 11 digitos",
                error.getReason()
        );

        verify(repository, never()).save(any(Cliente.class));
    }

    @Test
    void debeAceptarRucConEstructuraValida() {
        // RUC generado solo para probar el algoritmo.
        // No representa una empresa registrada en SUNAT.
        Cliente cliente = crearCliente("20123456786");

        when(repository.save(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cliente resultado = service.crearCliente(cliente);

        assertNotNull(resultado);
        assertEquals("20123456786", resultado.getNumeroDocumento());

        verify(repository, times(1)).save(any(Cliente.class));
    }
}
