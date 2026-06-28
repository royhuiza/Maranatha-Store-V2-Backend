package com.libreria.auth.service;

import com.libreria.auth.dto.UsuarioDTO;
import com.libreria.auth.model.Rol;
import com.libreria.auth.model.Usuario;
import com.libreria.auth.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository) {
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario registrarUsuario(UsuarioDTO dto) {
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setEmail(email);
        usuario.setRol(dto.getRol() == null ? Rol.USER : dto.getRol());
        return usuarioRepository.save(usuario);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Override
    public Usuario actualizarUsuario(Long id, UsuarioDTO dto) {
        Usuario usuario = obtenerUsuario(id);
        usuario.setUsername(dto.getUsername().trim());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        usuario.setEmail(dto.getEmail().trim().toLowerCase());
        usuario.setRol(dto.getRol() == null ? usuario.getRol() : dto.getRol());
        return usuarioRepository.save(usuario);
    }

    @Override
    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuario(id);
        usuarioRepository.delete(usuario);
    }
}
