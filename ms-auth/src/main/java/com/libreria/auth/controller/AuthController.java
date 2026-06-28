package com.libreria.auth.controller;

import com.libreria.auth.dto.AuthResponse;
import com.libreria.auth.dto.LoginRequest;
import com.libreria.auth.dto.UsuarioDTO;
import com.libreria.auth.jwt.JwtService;
import com.libreria.auth.model.Rol;
import com.libreria.auth.model.Usuario;
import com.libreria.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtService jwtService, AuthService authService,
                          AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        String role = authentication.getAuthorities().stream().findFirst()
                .map(Object::toString).orElse("ROLE_USER").replace("ROLE_", "");
        return new AuthResponse(jwtService.generateToken(authentication), authentication.getName(), role);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario")
    public ResponseEntity<Usuario> register(@Valid @RequestBody UsuarioDTO dto) {
        dto.setRol(Rol.USER);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarUsuario(dto));
    }

    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() { return authService.listarUsuarios(); }

    @GetMapping("/usuarios/{id}")
    public Usuario obtenerUsuario(@PathVariable Long id) { return authService.obtenerUsuario(id); }

    @PutMapping("/usuarios/{id}")
    public Usuario actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO dto) {
        return authService.actualizarUsuario(id, dto);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        authService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test")
    public String test() { return "MS-AUTH funcionando"; }
}
