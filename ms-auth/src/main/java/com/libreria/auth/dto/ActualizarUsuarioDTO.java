package com.libreria.auth.dto;

import com.libreria.auth.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioDTO(@NotBlank @Size(min=3,max=50) String username,
                                   @NotBlank @Email @Size(max=120) String email, @NotNull Rol rol,
                                   @Size(min=10,max=72) String password) {}
