package com.libreria.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsVentasApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsVentasApplication.class, args);
    }
}