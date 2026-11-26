package com.libreria.ventas.service;

import com.libreria.ventas.dto.CrearVentaRequest;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Demo exclusiva: stock y registro atomicos en db_ventas_test; emision fuera de la transaccion. */
@Service
@Profile("demo-fiscal")
@ConditionalOnProperty(name="maranatha.demo.integrado.enabled", havingValue="true")
public class CheckoutDemoIntegrado {
    private final JdbcTemplate jdbc;
    private final VentaRepository ventas;
    private final VentaEmisionBetaService fiscal;
    private final TransactionTemplate tx;
    public CheckoutDemoIntegrado(JdbcTemplate jdbc, VentaRepository ventas, VentaEmisionBetaService fiscal, PlatformTransactionManager tm) {
        this.jdbc=jdbc; this.ventas=ventas; this.fiscal=fiscal; this.tx=new TransactionTemplate(tm);
    }
    private void proteger() {
        if (!"db_ventas_test".equals(jdbc.queryForObject("select current_database()",String.class)))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Demo exige db_ventas_test");
    }
    private void preparar() {
        proteger();
        jdbc.execute("select pg_advisory_xact_lock(73002519)");
        jdbc.execute("create table if not exists demo_stock_integrado (producto_id bigint primary key, stock integer not null check(stock>=0))");
        jdbc.execute("create table if not exists demo_checkout_integrado (venta_id bigint primary key references ventas(id))");
        jdbc.execute("create table if not exists demo_comprador_local (venta_id bigint primary key references demo_checkout_integrado(venta_id), nombre varchar(220) not null, documento_parcial varchar(40) not null)");
        for(var p:CatalogoBeta.copiaAutorizada()) jdbc.update("insert into demo_stock_integrado values (?,100) on conflict do nothing",p.id());
    }
    public List<Map<String,Object>> catalogo(String q, boolean bajo) {
        return tx.execute(st -> {
            preparar();
            Map<Long,Integer> stocks=new HashMap<>();
            jdbc.query("select producto_id,stock from demo_stock_integrado",rs->{stocks.put(rs.getLong(1),rs.getInt(2));});
            String filtro=q==null?"":q.toLowerCase(Locale.ROOT).trim();
            return CatalogoBeta.copiaAutorizada().stream().sorted(Comparator.comparingLong(CatalogoBeta.Linea::id))
                .filter(p-> !bajo || stocks.get(p.id())<=5)
                .filter(p->p.nombre().toLowerCase(Locale.ROOT).contains(filtro)||String.valueOf(p.id()).contains(filtro))
                .map(p->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.id());m.put("codigo","DEMO-"+p.id());m.put("nombre",p.nombre().replaceFirst("^PRUEBA \\[\\d+\\] ",""));m.put("descripcion","Catalogo autorizado de demostracion");m.put("precio",p.precio());m.put("stock",stocks.get(p.id()));m.put("activo",true);m.put("afectacionIgv",p.igv());return m;}).toList();
        });
    }
    private record CompradorLocal(String nombre,String documentoParcial) {}
    private CompradorLocal comprador(CrearVentaRequest r) {
        if(r.getTipoComprobante()==TipoComprobante.NOTA_PEDIDO)return null;
        String tipo=r.getTipoComprobante()==TipoComprobante.FACTURA?"RUC":"DNI";
        String doc=Objects.toString(r.getNumeroDocumento(),"").trim();
        String nombre=Objects.toString(r.getClienteNombre(),"").trim();
        // Admitir clientes sin dato local en pruebas anteriores; nunca reconstruir identidades.
        if(doc.isEmpty()&&nombre.isEmpty())return null;
        if(!tipo.equals(r.getTipoDocumento()) || !doc.matches(tipo.equals("RUC")?"[0-9]{11}":"[0-9]{8}") || nombre.isEmpty() || nombre.length()>220 || nombre.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Datos locales del comprador invalidos");
        if(r.getDocumento()!=null&&!r.getDocumento().isBlank()&&!doc.equals(r.getDocumento().trim()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Documento local inconsistente");
        return new CompradorLocal(nombre,tipo+" "+"*".repeat(doc.length()-4)+doc.substring(doc.length()-4));
    }
    private boolean existeTablaComprador(){return Boolean.TRUE.equals(jdbc.queryForObject("select to_regclass('public.demo_comprador_local') is not null",Boolean.class));}
    private CompradorLocal leerComprador(Long id){
        if(!existeTablaComprador())return null;
        return jdbc.query("select nombre,documento_parcial from demo_comprador_local where venta_id=?",(rs,n)->new CompradorLocal(rs.getString(1),rs.getString(2)),id).stream().findFirst().orElse(null);
    }
    // Solo se llama para respuestas de la API demo autorizada; no durante el envio fiscal.
    public Venta adjuntarComprador(Venta v){
        proteger();
        var c=leerComprador(v.getId());
        if(c!=null)v.setCompradorLocal(c.nombre(),c.documentoParcial());
        return v;
    }
    public void adjuntarCompradores(List<Venta> lista){
        proteger();
        if(!existeTablaComprador()||lista.isEmpty())return;
        Map<Long,Venta> porId=new HashMap<>(); for(var v:lista)porId.put(v.getId(),v);
        // Consulta acotada a las ventas retornadas, sin exponer claves ni documentos completos.
        var ids=porId.keySet().toArray();
        var marcas=String.join(",",Collections.nCopies(ids.length,"?"));
        jdbc.query("select venta_id,nombre,documento_parcial from demo_comprador_local where venta_id in ("+marcas+")",rs->{var v=porId.get(rs.getLong(1));if(v!=null)v.setCompradorLocal(rs.getString(2),rs.getString(3));},ids);
    }
    public Venta confirmar(CrearVentaRequest r) {
        proteger();
        final UUID uuid;
        final List<CatalogoBeta.Linea> lineas;
        final String clave;
        try {
            uuid=UUID.fromString(r.getIdempotencyKey());
            if(!uuid.toString().equalsIgnoreCase(r.getIdempotencyKey())||r.getTipoComprobante()==null)throw new IllegalArgumentException();
            Map<Long,Integer> items=new TreeMap<>();
            for(var d:r.getDetalles())items.merge(d.getProductoId(),d.getCantidad(),Math::addExact);
            lineas=CatalogoBeta.lineasCheckout(items); clave="DEMO-FISCAL-"+uuid;
        }catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Carrito de prueba o clave invalida");}
        final CompradorLocal comprador=comprador(r);
        // La identidad fiscal sigue siendo sintetica. El dato local no entra al XML ni al QR.
        if(r.getMetodoPago()!=null && r.getMetodoPago()!=MetodoPago.EFECTIVO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Demo: pago simulado en efectivo solamente");
        Venta v=tx.execute(st->{
            preparar();
            var anterior=ventas.findByIdempotencyKey(clave).orElse(null);
            if(anterior!=null){
                if(!esCheckout(anterior.getId())||anterior.getTipoComprobante()!=r.getTipoComprobante()||anterior.getDetalles().size()!=lineas.size()||!lineas.stream().allMatch(x->anterior.getDetalles().stream().anyMatch(d->Objects.equals(d.getProductoId(),x.id())&&Objects.equals(d.getCantidad(),x.cantidad())&&d.getPrecioUnitario().compareTo(x.precio())==0)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Clave de otra operacion");
                if(!Objects.equals(leerComprador(anterior.getId()),comprador))throw new ResponseStatusException(HttpStatus.CONFLICT,"Clave de otro comprador local; revise historial");
                return anterior;
            }
            if(Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from demo_checkout_integrado c join ventas v on v.id=c.venta_id where v.estado='EMITIDA' and v.estado_sunat in ('PENDIENTE','EN_PROCESO','ERROR_COMUNICACION','REQUIERE_REVISION'))",Boolean.class)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Hay un comprobante demo pendiente de revision. No repetir ni iniciar otra venta hasta conciliarlo.");
            for(var x:lineas)if(jdbc.update("update demo_stock_integrado set stock=stock-? where producto_id=? and stock>=?",x.cantidad(),x.id(),x.cantidad())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Stock de prueba insuficiente");
            Venta nueva=new Venta(); nueva.setIdempotencyKey(clave); nueva.setClienteNombre("EMPRESA FICTICIA DE PRUEBA"); nueva.setClienteDocumento("RUC 20123456786");
            nueva.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));nueva.setFechaZona("America/Lima");nueva.setTipoComprobante(r.getTipoComprobante());
            nueva.setSerie(switch(r.getTipoComprobante()){case FACTURA->"F9B1";case BOLETA->"B9B1";case NOTA_PEDIDO->"NP9B";});
            nueva.setNumero("PENDIENTE");nueva.setEstado(EstadoVenta.EMITIDA);nueva.setEstadoPago("SIMULADO");nueva.setMetodoPago(MetodoPago.EFECTIVO);nueva.setValidadoSunat(false);
            nueva.setEstadoSunat(r.getTipoComprobante()==TipoComprobante.NOTA_PEDIDO?EstadoSunat.NO_APLICA:EstadoSunat.PENDIENTE);
            BigDecimal total=BigDecimal.ZERO;
            for(var x:lineas){var d=new VentaDetalle();d.setProductoId(x.id());d.setProductoNombre(x.nombre());d.setCantidad(x.cantidad());d.setPrecioUnitario(x.precio());d.setAfectacionIgv(x.igv());d.setSubtotal(x.precio().multiply(BigDecimal.valueOf(x.cantidad())));total=total.add(d.getSubtotal());nueva.agregarDetalle(d);}
            nueva.setTotal(total);ventas.saveAndFlush(nueva);nueva.setNumero(String.format(Locale.ROOT,"%08d",nueva.getId()));ventas.saveAndFlush(nueva);
            jdbc.update("insert into demo_checkout_integrado values (?)",nueva.getId());
            if(comprador!=null)jdbc.update("insert into demo_comprador_local(venta_id,nombre,documento_parcial) values (?,?,?)",nueva.getId(),comprador.nombre(),comprador.documentoParcial());
            return nueva;
        });
        if(v==null)throw new IllegalStateException("Registro de prueba no confirmado");
        // Emision reservada de forma atomica por VentaFiscalService. No se reenvia un resultado incierto.
        if(v.getEstado()==EstadoVenta.EMITIDA && v.getEstadoSunat()==EstadoSunat.PENDIENTE && v.getTipoComprobante()!=TipoComprobante.NOTA_PEDIDO)return adjuntarComprador(fiscal.emitirFacturaPrueba(v.getId(),"20123456786"));
        return adjuntarComprador(v);
    }
    private boolean esCheckout(Long id){return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from demo_checkout_integrado where venta_id=?)",Boolean.class,id));}
    public Venta anular(Long id){
        return tx.execute(st->{preparar();
            var v=ventas.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada"));
            if(!esCheckout(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Evidencia anterior: no tiene movimientos de stock de esta demo");
            if(v.getEstado()==EstadoVenta.ANULADA)return v;
            if(v.getEstadoSunat()!=EstadoSunat.ACEPTADO_BETA && v.getEstadoSunat()!=EstadoSunat.NO_APLICA)throw new ResponseStatusException(HttpStatus.CONFLICT,"Revise el resultado fiscal antes de anular");
            for(var d:v.getDetalles())if(jdbc.update("update demo_stock_integrado set stock=stock+? where producto_id=?",d.getCantidad(),d.getProductoId())!=1)throw new IllegalStateException("Stock demo no encontrado");
            v.setEstado(EstadoVenta.ANULADA);return ventas.saveAndFlush(v);
        });
    }
}
