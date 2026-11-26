package com.libreria.ventas.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** La instancia de laboratorio no debe ejecutar las rutas comerciales. */
@Component
@ConditionalOnProperty(name="maranatha.laboratorio.aislado", havingValue="true")
public class LaboratorioAislamientoFilter extends OncePerRequestFilter {
    @org.springframework.beans.factory.annotation.Value("${maranatha.demo.checkout.enabled:false}")
    private boolean checkoutDemo;
    @org.springframework.beans.factory.annotation.Value("${maranatha.demo.integrado.enabled:false}")
    private boolean demoIntegrado;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String path=request.getServletPath();
        String metodo=request.getMethod();
        if(demoIntegrado){
            boolean lectura="GET".equals(metodo) && (path.equals("/ventas")||path.equals("/ventas/historial")||path.matches("/ventas/\\d+(/estado-sunat|/comprobante-local)?")||path.matches("/ventas/demo/(catalogo(/bajo|/\\d+)?|clientes|dni/\\d{8}|ruc/\\d{11})")||path.equals("/actuator/health"));
            boolean escritura=("POST".equals(metodo)&&path.equals("/ventas"))||("PUT".equals(metodo)&&path.matches("/ventas/\\d+/anular"));
            if(lectura||escritura)chain.doFilter(request,response);else response.sendError(404);
            return;
        }
        if ((checkoutDemo && "POST".equals(request.getMethod()) && "/ventas".equals(path))
            || ("POST".equals(request.getMethod()) && path.matches("/ventas/laboratorio/emision-beta/[0-9a-fA-F-]{36}"))
            || ("GET".equals(request.getMethod()) && "/actuator/health".equals(path))) {
            chain.doFilter(request,response);
        } else response.sendError(404);
    }
}
