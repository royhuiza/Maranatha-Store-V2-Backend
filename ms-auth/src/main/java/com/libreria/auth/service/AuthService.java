package com.libreria.auth.service;

import com.libreria.auth.dto.UsuarioDTO;
import com.libreria.auth.model.Usuario;
import java.util.List;

public interface AuthService {
    Usuario registrarUsuario(UsuarioDTO usuarioDTO);
    List<Usuario> listarUsuarios();
    Usuario obtenerUsuario(Long id);
    Usuario actualizarUsuario(Long id, UsuarioDTO usuarioDTO);
    void eliminarUsuario(Long id);
}