package com.libreria.auth.config;

import com.libreria.auth.model.Rol;
import com.libreria.auth.model.Usuario;
import com.libreria.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createDefaultAdmin(UsuarioRepository repository, PasswordEncoder encoder) {
        return args -> repository.findOptionalByUsernameIgnoreCase("admin")
                .ifPresentOrElse(admin -> {
                    if (admin.getRol() != Rol.ADMIN) {
                        admin.setRol(Rol.ADMIN);
                        repository.save(admin);
                    }
                }, () -> {
                    Usuario admin = new Usuario();
                    admin.setUsername("admin");
                    admin.setPassword(encoder.encode("admin123"));
                    admin.setEmail("admin@maranatha.local");
                    admin.setRol(Rol.ADMIN);
                    repository.save(admin);
                });
    }
}
