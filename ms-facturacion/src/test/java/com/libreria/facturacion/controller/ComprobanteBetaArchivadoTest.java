package com.libreria.facturacion.controller;
import com.libreria.facturacion.service.QrFiscalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ComprobanteBetaArchivadoTest {
 @TempDir Path dir;
 private Path preparar() throws Exception {
  Path p=dir.resolve("beta/20123456786-01-F9B1-00000010"); Files.createDirectories(p);
  for(String n:new String[]{"factura.xml","cdr.xml"}) try(var in=getClass().getResourceAsStream("/laboratorio/"+n)){ assertNotNull(in); Files.copy(in,p.resolve(n)); }
  return p;
 }
 @Test void evidenciaCoincidente() throws Exception {
  preparar(); var v=new ComprobanteBetaArchivadoController(new QrFiscalService(),dir.toString()).consultar("00000010");
  assertEquals("ACEPTADO_BETA",v.estadoSunat()); assertFalse(v.validezFiscal()); assertEquals("0",v.codigo()); assertEquals("11.80",v.total()); assertEquals("10.00",v.subtotal()); assertEquals("EMPRESA FICTICIA DE PRUEBA",v.cliente()); assertEquals(1,v.detalles().size()); assertEquals("Producto de prueba",v.detalles().get(0).descripcion()); assertEquals("11.80",v.detalles().get(0).importe());
  assertEquals("20123456786|01|F9B1|00000010|1.80|11.80|2026-11-24|6|20123456786|kcG6BTC+Z3/K9TU2M0/qQtSig/KNXnRw2EM91Wy6PFk=",v.contenidoQr());
 }
 @Test void rechazoCdrDistinto() throws Exception {
  Path p=preparar().resolve("cdr.xml"); Files.writeString(p,Files.readString(p).replace("F9B1-00000010","F9B1-00000011"));
  var c=new ComprobanteBetaArchivadoController(new QrFiscalService(),dir.toString()); assertEquals(409,assertThrows(ResponseStatusException.class,()->c.consultar("00000010")).getStatusCode().value());
 }
 @Test void rechazoXmlAlterado() throws Exception {
  Path p=preparar().resolve("factura.xml"); Files.writeString(p,Files.readString(p).replace("Producto de prueba","Producto alterado"));
  var c=new ComprobanteBetaArchivadoController(new QrFiscalService(),dir.toString()); assertEquals(409,assertThrows(ResponseStatusException.class,()->c.consultar("00000010")).getStatusCode().value());
 }
 @Test void faltante() { var c=new ComprobanteBetaArchivadoController(new QrFiscalService(),dir.toString()); assertEquals(404,assertThrows(ResponseStatusException.class,()->c.consultar("00000010")).getStatusCode().value()); }
}
