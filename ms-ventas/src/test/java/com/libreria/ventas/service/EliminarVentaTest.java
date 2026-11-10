package com.libreria.ventas.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class EliminarVentaTest {
 @Test void deletesOnlyInternalAnnulledSale(){var repo=mock(VentaRepository.class);var v=sale(EstadoVenta.ANULADA);when(repo.findLockedById(1L)).thenReturn(Optional.of(v));new VentaServiceImpl(repo,null,null,new ObjectMapper(),null).eliminarVenta(1L);verify(repo).delete(v);verify(repo).flush();}
 @Test void rejectsEmittedSale(){var repo=mock(VentaRepository.class);when(repo.findLockedById(1L)).thenReturn(Optional.of(sale(EstadoVenta.EMITIDA)));assertThrows(ResponseStatusException.class,()->new VentaServiceImpl(repo,null,null,new ObjectMapper(),null).eliminarVenta(1L));verify(repo,never()).delete(any());}
 private Venta sale(EstadoVenta state){var v=new Venta();v.setEstado(state);return v;}
}
