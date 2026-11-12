package com.libreria.auth.controller;
import com.libreria.auth.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class PasswordController {
    private final UsuarioRepository users;
    private final PasswordEncoder encoder;
    public PasswordController(UsuarioRepository users, PasswordEncoder encoder) { this.users=users; this.encoder=encoder; }
    public record PasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min=10,max=72) String newPassword) {}
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void change(Authentication auth, @Valid @RequestBody PasswordRequest body) {
        var user=users.findOptionalByUsernameIgnoreCase(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!encoder.matches(body.currentPassword(), user.getPassword())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Contraseña actual incorrecta");
        if (body.currentPassword().equals(body.newPassword())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Elija una contraseña diferente");
        user.setPassword(encoder.encode(body.newPassword())); users.save(user);
    }
}
