package com.libreria.auth.controller;
import com.libreria.auth.dto.LoginRequest;
import com.libreria.auth.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoleResponseTest {
 @Test void roleComesFromAuthorityValueNotLambdaToString(){
   GrantedAuthority authority=()->"ROLE_ADMIN";
   var authenticated=new UsernamePasswordAuthenticationToken("admin-prueba",null,List.of(authority));
   AuthenticationManager manager=request->authenticated;
   var jwt=new JwtService("MaranathaStoreClaveSuperSeguraJWT2026Maranatha",60000);
   var controller=new AuthController(jwt,null,manager);
   var request=new LoginRequest();request.setUsername("admin-prueba");request.setPassword("unit-test-only");
   var response=controller.login(request);
   assertEquals("ADMIN",response.rol());
   assertEquals("ROLE_ADMIN",jwt.parse(response.token()).get("role",String.class));
 }
 @Test void sessionReportsActualAuthorityValue(){
   var authenticated=new UsernamePasswordAuthenticationToken("vendedor-prueba",null,List.of((GrantedAuthority)()->"ROLE_USER"));
   assertEquals("USER",new SessionController().me(authenticated).rol());
 }
}
