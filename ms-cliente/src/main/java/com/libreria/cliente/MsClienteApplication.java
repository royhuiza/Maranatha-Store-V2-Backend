package com.libreria.cliente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsClienteApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsClienteApplication.class, args);
    }
}