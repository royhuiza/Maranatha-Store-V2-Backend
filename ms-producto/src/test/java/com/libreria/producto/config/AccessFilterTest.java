package com.libreria.producto.config;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import jakarta.servlet.FilterChain;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccessFilterTest {
 private final String secret="MaranathaStoreClaveSuperSeguraJWT2026Maranatha";
 @Test void directAnonymousRequestIsRejected()throws Exception{check(null,401,false);}
 @Test void sellerCannotModifyInventoryEvenDirectly()throws Exception{check("ROLE_USER",403,false);}
 @Test void adminCanModifyInventory()throws Exception{check("ROLE_ADMIN",200,true);}
 private void check(String role,int status,boolean allowed)throws Exception{var req=new MockHttpServletRequest("POST","/productos");if(role!=null){var jwt=Jwts.builder().setSubject("test").claim("role",role).setExpiration(new Date(System.currentTimeMillis()+60000)).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();req.addHeader("Authorization","Bearer "+jwt);}var res=new MockHttpServletResponse();var chain=mock(FilterChain.class);new AccessFilter(secret).doFilter(req,res,chain);assertEquals(status,res.getStatus());if(allowed)verify(chain).doFilter(req,res);else verifyNoInteractions(chain);}
}
