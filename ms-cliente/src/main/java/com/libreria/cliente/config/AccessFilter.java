package com.libreria.cliente.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AccessFilter extends OncePerRequestFilter {
 private final javax.crypto.SecretKey key;
 public AccessFilter(@Value("${jwt.secret:MaranathaStoreClaveSuperSeguraJWT2026Maranatha}") String secret) { this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
   String p=req.getRequestURI(), method=req.getMethod();
   if(!p.startsWith("/clientes")){chain.doFilter(req,res);return;}
   String service=req.getHeader("X-Internal-Token");
   boolean internalPath = method.equals("GET") || false;
   if(internalPath && service!=null){
     try { var c=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(service).getBody();
       if("ms-ventas".equals(c.getSubject()) && "INTERNAL_STOCK".equals(c.get("role",String.class)) && c.getExpiration()!=null){chain.doFilter(req,res);return;}
     }catch(Exception ignored){} }

   String header=req.getHeader("Authorization"); String role;
   try {
     if(header==null || !header.startsWith("Bearer "))throw new IllegalArgumentException();
     var claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(header.substring(7)).getBody();
     role=claims.get("role",String.class);
     if(claims.getSubject()==null || claims.getExpiration()==null || !("ROLE_ADMIN".equals(role)||"ROLE_USER".equals(role)))throw new IllegalArgumentException();
   }catch(Exception e){deny(res,401,"Token requerido o inválido");return;}
   boolean adminOnly=method.equals("DELETE");
   if(adminOnly && !"ROLE_ADMIN".equals(role)){deny(res,403,"Operación reservada al administrador");return;}
   chain.doFilter(req,res);
 }
 private void deny(HttpServletResponse res,int status,String message)throws IOException{res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"status\":"+status+",\"message\":\""+message+"\"}");}
}
