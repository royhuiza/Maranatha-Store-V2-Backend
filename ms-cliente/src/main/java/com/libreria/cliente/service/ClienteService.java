package com.libreria.cliente.service;

import com.libreria.cliente.model.Cliente;
import java.util.List;

public interface ClienteService {

    List<Cliente> listarClientes();

    Cliente obtenerCliente(Long id);

    Cliente crearCliente(Cliente cliente);

    Cliente actualizarCliente(Long id, Cliente cliente);

    void eliminarCliente(Long id);
}