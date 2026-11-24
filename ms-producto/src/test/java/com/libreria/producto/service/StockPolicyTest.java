package com.libreria.producto.service;
import com.libreria.producto.model.Producto;
import com.libreria.producto.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class StockPolicyTest {
 @Test void blocksZeroAndDoesNotWrite(){var repo=mock(ProductoRepository.class);var p=product(0);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(p));assertThrows(ResponseStatusException.class,()->new ProductoServiceImpl(repo, null, null, null).descontarStock(1L,1));verify(repo,never()).save(any());}
 @Test void lowStockCanBeSoldAndStockIsReduced(){var repo=mock(ProductoRepository.class);var p=product(2);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(p));when(repo.save(p)).thenReturn(p);assertEquals(0,new ProductoServiceImpl(repo, null, null, null).descontarStock(1L,2).getStock());}
 @Test void cannotSellMoreThanAvailable(){var repo=mock(ProductoRepository.class);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(product(6)));assertThrows(ResponseStatusException.class,()->new ProductoServiceImpl(repo, null, null, null).descontarStock(1L,7));verify(repo,never()).save(any());}
 @Test void deleteDeactivatesInsteadOfRemoving(){var repo=mock(ProductoRepository.class);var p=product(0);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(p));new ProductoServiceImpl(repo, null, null, null).eliminarProducto(1L);assertFalse(p.getActivo());verify(repo).save(p);verify(repo,never()).delete(any());}
 @Test void deleteWithStockIsRejected(){var repo=mock(ProductoRepository.class);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(product(3)));assertThrows(ResponseStatusException.class,()->new ProductoServiceImpl(repo, null, null, null).eliminarProducto(1L));verify(repo,never()).save(any());}
 private Producto product(int stock){var p=new Producto();p.setStock(stock);p.setActivo(true);p.setNombre("Cuaderno");return p;}
}
