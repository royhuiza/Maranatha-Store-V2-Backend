package com.libreria.facturacion.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class InternalTokenService {

    private final String tokenConfigurado;


    public InternalTokenService(
            @Value("${facturacion.internal.token:}")
            String tokenConfigurado
    ) {
        this.tokenConfigurado = tokenConfigurado;
    }

    public boolean validar(String tokenRecibido) {

        if (tokenConfigurado == null
                || tokenConfigurado.isBlank()
                || tokenRecibido == null
                || tokenRecibido.isBlank()) {

            return false;
        }

        return MessageDigest.isEqual(
                tokenConfigurado.getBytes(
                        StandardCharsets.UTF_8),
                tokenRecibido.getBytes(
                        StandardCharsets.UTF_8)
        );
    }

}
