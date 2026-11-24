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
        if (username.length()<3 || dto.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usuario o contraseña inválidos");
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
        usuario.setRol(dto.getRol() == null ? Rol.VENDEDOR : dto.getRol());
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

    private void bloquearCambios() { usuarioRepository.bloquearUsuarios(); }
    private boolean propia(Usuario u) {
        return u.getUsername().equalsIgnoreCase(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
    }
    private void proteger(Usuario u) {
        if (propia(u)) throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar o cambiar el rol de tu propia cuenta");
        if (u.isActivo() && u.getRol() == Rol.ADMIN && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe permanecer un administrador activo");
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public Usuario actualizarUsuario(Long id, com.libreria.auth.dto.ActualizarUsuarioDTO dto) {
        bloquearCambios();
        Usuario u = obtenerUsuario(id);
        String nombre = dto.username().trim(); String correo = dto.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (nombre.length()<3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usuario inválido");
        if (propia(u) && !u.getUsername().equals(nombre)) throw new ResponseStatusException(HttpStatus.CONFLICT,"No puedes cambiar tu propio nombre de acceso");
        if (u.getRol()!=dto.rol()) proteger(u);
        if (usuarioRepository.findOptionalByUsernameIgnoreCase(nombre).filter(x -> !x.getId().equals(id)).isPresent() || usuarioRepository.findOptionalByEmailIgnoreCase(correo).filter(x -> !x.getId().equals(id)).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Usuario o correo ya registrado");
        if (!u.getUsername().equals(nombre) || u.getRol()!=dto.rol() || dto.password()!=null) u.invalidarSesiones();
        u.setUsername(nombre); u.setEmail(correo); u.setRol(dto.rol());
        if(dto.password()!=null) { if(dto.password().isBlank() || dto.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Contraseña inválida"); u.setPassword(passwordEncoder.encode(dto.password())); }
        return usuarioRepository.save(u);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarUsuario(Long id) { bloquearCambios(); Usuario u=obtenerUsuario(id); proteger(u); u.invalidarSesiones(); u.setActivo(false); usuarioRepository.save(u); }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public Usuario reactivarUsuario(Long id) { bloquearCambios(); Usuario u=obtenerUsuario(id); u.setActivo(true); return usuarioRepository.save(u); }
}
