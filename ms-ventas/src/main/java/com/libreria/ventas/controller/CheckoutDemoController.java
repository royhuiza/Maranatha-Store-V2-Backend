package com.libreria.ventas.controller;
import com.libreria.ventas.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController
@RequestMapping("/ventas/demo")
@Profile("demo-fiscal")
@ConditionalOnProperty(name="maranatha.demo.integrado.enabled",havingValue="true")
public class CheckoutDemoController {
    private final CheckoutDemoIntegrado demo;
    public CheckoutDemoController(CheckoutDemoIntegrado demo){this.demo=demo;}
    @GetMapping("/catalogo") public List<Map<String,Object>> catalogo(@RequestParam(name="q",required=false) String q){return demo.catalogo(q,false);}
    @GetMapping("/catalogo/bajo") public List<Map<String,Object>> bajo(){return demo.catalogo(null,true);}
    @GetMapping("/catalogo/{id}") public Map<String,Object> producto(@PathVariable long id){return demo.catalogo(null,false).stream().filter(m->Objects.equals(m.get("id"),id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto de prueba no encontrado"));}
    @GetMapping("/clientes") public List<Object> clientes(){return List.of();}
    @GetMapping("/dni/{dni}") public Map<String,String> dni(@PathVariable String dni){if(!"00000000".equals(dni))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solo DNI sintetico 00000000 en esta demo");return Map.of("numeroDocumento",dni,"nombres","COMPRADOR","apellidoPaterno","DE PRUEBA","apellidoMaterno","","estado","DEMO");}
    @GetMapping("/ruc/{ruc}") public Map<String,String> ruc(@PathVariable String ruc){if(!"20123456786".equals(ruc))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solo RUC de laboratorio en esta demo");return Map.of("ruc",ruc,"razonSocial","EMPRESA FICTICIA DE PRUEBA","estado","DEMO","condicion","DEMO","direccionFiscal","DIRECCION DE PRUEBA");}
}
