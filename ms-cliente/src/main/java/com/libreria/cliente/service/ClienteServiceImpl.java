
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado"
                ));
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

        if (cambios.getActivo() != null) {
            cliente.setActivo(cambios.getActivo());
        }

        return repository.save(cliente);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        repository.delete(obtenerCliente(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerClientePorDocumento(String tipoDocumento, String numeroDocumento) {
        if (tipoDocumento == null || numeroDocumento == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo y numero de documento son obligatorios"
            );
        }
        Cliente encontrado = repository.findByTipoDocumentoAndNumeroDocumento(
                tipoDocumento.toUpperCase(java.util.Locale.ROOT),
                numeroDocumento.trim()
        );
        if (encontrado == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cliente no encontrado con documento " + tipoDocumento + " " + numeroDocumento
            );
        }
        return encontrado;
    }

    private void normalizar(Cliente cliente) {
        String nombre = nullable(cliente.getNombre());

        if (nombre == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio"
            );
        }

        cliente.setNombre(nombre);
        cliente.setApellido(nullable(cliente.getApellido()));

        String tipo = nullable(cliente.getTipoDocumento());

        cliente.setTipoDocumento(
                tipo == null ? "DNI" : tipo.toUpperCase(Locale.ROOT)
        );

        cliente.setNumeroDocumento(
                nullable(cliente.getNumeroDocumento())
        );

        cliente.setEmail(nullable(cliente.getEmail()));

        if (cliente.getEmail() != null) {
            cliente.setEmail(
                    cliente.getEmail().toLowerCase(Locale.ROOT)
            );
        }

        cliente.setTelefono(nullable(cliente.getTelefono()));
        cliente.setDireccion(nullable(cliente.getDireccion()));

        if (cliente.getActivo() == null) {
            cliente.setActivo(true);
        }
    }

    private void validarDocumento(Cliente cliente) {
        String tipo = cliente.getTipoDocumento();
        String numero = cliente.getNumeroDocumento();

        if (numero == null) {
            if ("RUC".equals(tipo)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El RUC es obligatorio"
                );
            }
            return;
        }

        if ("DNI".equals(tipo)) {
            if (!numero.matches("\\d{8}")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El DNI debe tener exactamente 8 digitos"
                );
            }
        }

        if ("RUC".equals(tipo)) {
            if (!numero.matches("\\d{11}")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El RUC debe tener exactamente 11 digitos"
                );
            }

            if (!validarRucPeruano(numero)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El RUC tiene un digito verificador incorrecto"
                );
            }
        }
    }

    private boolean validarRucPeruano(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            return false;
        }

        String prefijo = ruc.substring(0, 2);

        if (!prefijo.equals("10")
                && !prefijo.equals("15")
                && !prefijo.equals("16")
                && !prefijo.equals("17")
                && !prefijo.equals("20")) {
            return false;
        }

        int[] factores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

        int suma = 0;

        for (int i = 0; i < factores.length; i++) {
            int digito = Character.digit(ruc.charAt(i), 10);
            suma += digito * factores[i];
        }

        int residuo = suma % 11;
        int verificador = 11 - residuo;

        if (verificador == 10) {
            verificador = 0;
        } else if (verificador == 11) {
            verificador = 1;
        }

        int ultimoDigito = Character.digit(ruc.charAt(10), 10);

        return verificador == ultimoDigito;
    }

    private void validarDuplicados(Cliente cliente, Long id) {
        if (cliente.getEmail() != null) {
            boolean existe = id == null
                    ? repository.existsByEmailIgnoreCase(cliente.getEmail())
                    : repository.existsByEmailIgnoreCaseAndIdNot(
                    cliente.getEmail(), id
            );

            if (existe) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El correo ya esta registrado"
                );
            }
        }

        if (cliente.getNumeroDocumento() != null) {
            boolean existe = id == null
                    ? repository.existsByNumeroDocumento(
                    cliente.getNumeroDocumento()
            )
                    : repository.existsByNumeroDocumentoAndIdNot(
                    cliente.getNumeroDocumento(), id
            );

            if (existe) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El documento ya esta registrado"
                );
            }
        }
    }

    private String nullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
