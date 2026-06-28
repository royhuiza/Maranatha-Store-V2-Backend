package com.libreria.cliente.service;

import com.libreria.cliente.model.Cliente;
import com.libreria.cliente.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository repository;

    public ClienteServiceImpl(ClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerCliente(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    @Override
    @Transactional
    public Cliente crearCliente(Cliente cliente) {
        cliente.setId(null);
        normalizar(cliente);
        validarDocumento(cliente);
        validarDuplicados(cliente, null);
        return repository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente cambios) {
        Cliente cliente = obtenerCliente(id);
        cambios.setId(id);
        normalizar(cambios);
        validarDocumento(cambios);
        validarDuplicados(cambios, id);
        cliente.setNombre(cambios.getNombre());
        cliente.setApellido(cambios.getApellido());
        cliente.setTipoDocumento(cambios.getTipoDocumento());
        cliente.setNumeroDocumento(cambios.getNumeroDocumento());
        cliente.setEmail(cambios.getEmail());
        cliente.setTelefono(cambios.getTelefono());
        cliente.setDireccion(cambios.getDireccion());
        if (cambios.getActivo() != null) cliente.setActivo(cambios.getActivo());
        return repository.save(cliente);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        repository.delete(obtenerCliente(id));
    }

    private void normalizar(Cliente cliente) {
        cliente.setNombre(cliente.getNombre().trim());
        cliente.setApellido(nullable(cliente.getApellido()));
        String tipo = nullable(cliente.getTipoDocumento());
        cliente.setTipoDocumento(tipo == null ? "DNI" : tipo.toUpperCase(Locale.ROOT));
        cliente.setNumeroDocumento(nullable(cliente.getNumeroDocumento()));
        cliente.setEmail(nullable(cliente.getEmail()));
        if (cliente.getEmail() != null) cliente.setEmail(cliente.getEmail().toLowerCase(Locale.ROOT));
        cliente.setTelefono(nullable(cliente.getTelefono()));
        cliente.setDireccion(nullable(cliente.getDireccion()));
        if (cliente.getActivo() == null) cliente.setActivo(true);
    }

    private void validarDocumento(Cliente cliente) {
        String numero = cliente.getNumeroDocumento();
        if (numero == null) return;
        if ("DNI".equals(cliente.getTipoDocumento()) && !numero.matches("\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI debe tener 8 dígitos");
        }
        if ("RUC".equals(cliente.getTipoDocumento()) && !numero.matches("\\d{11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RUC debe tener 11 dígitos");
        }
    }

    private void validarDuplicados(Cliente cliente, Long id) {
        if (cliente.getEmail() != null) {
            boolean existe = id == null ? repository.existsByEmailIgnoreCase(cliente.getEmail())
                    : repository.existsByEmailIgnoreCaseAndIdNot(cliente.getEmail(), id);
            if (existe) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        if (cliente.getNumeroDocumento() != null) {
            boolean existe = id == null ? repository.existsByNumeroDocumento(cliente.getNumeroDocumento())
                    : repository.existsByNumeroDocumentoAndIdNot(cliente.getNumeroDocumento(), id);
            if (existe) throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya está registrado");
        }
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
