package com.libreria.facturacion.service;
import java.math.BigDecimal;
import java.util.*;
/** Catalogo autorizado de demostracion: copia local, no consulta compradores ni stock comercial. */
public final class CatalogoBeta {
    private CatalogoBeta() {}
    public record Linea(long id, String nombre, BigDecimal precio, int cantidad, String igv) {}
    private static final Map<Long,Linea> COPIA = Map.ofEntries(
// INICIO COPIA AUTORIZADA
        Map.entry(1L,new Linea(1L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxXSBDdWFkZXJubyBjdWFkcmljdWxhZG8gQTQ="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("13.00"),1,"GRAVADO")),
        Map.entry(2L,new Linea(2L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyXSBDdWFkZXJubyB0cmlwbGUgcmF5YQ=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("8.50"),1,"GRAVADO")),
        Map.entry(3L,new Linea(3L,new String(Base64.getDecoder().decode("UFJVRUJBIFszXSBMYXBpY2VybyBhenVs"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(4L,new Linea(4L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0XSBMYXBpY2VybyBuZWdybw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(5L,new Linea(5L,new String(Base64.getDecoder().decode("UFJVRUJBIFs1XSBDdWFkZXJubyByYXlhZG8gQTQ="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("12.50"),1,"GRAVADO")),
        Map.entry(6L,new Linea(6L,new String(Base64.getDecoder().decode("UFJVRUJBIFs2XSBDdWFkZXJubyB1bml2ZXJzaXRhcmlv"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("18.00"),1,"GRAVADO")),
        Map.entry(7L,new Linea(7L,new String(Base64.getDecoder().decode("UFJVRUJBIFs3XSBMw6FwaXogSEI="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.00"),1,"GRAVADO")),
        Map.entry(8L,new Linea(8L,new String(Base64.getDecoder().decode("UFJVRUJBIFs4XSBMYXBpY2VybyByb2pv"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(9L,new Linea(9L,new String(Base64.getDecoder().decode("UFJVRUJBIFs5XSBCb3JyYWRvciBibGFuY28="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.00"),1,"GRAVADO")),
        Map.entry(10L,new Linea(10L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxMF0gTMOhcGl6IDJC"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(11L,new Linea(11L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxMV0gVGFqYWRvciBtZXTDoWxpY28="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(12L,new Linea(12L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxMl0gVGFqYWRvciBjb24gZGVww7NzaXRv"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("2.50"),1,"EXONERADO")),
        Map.entry(13L,new Linea(13L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxM10gRXNjdWFkcmEgZGUgNDXCsA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.50"),1,"INAFECTO")),
        Map.entry(14L,new Linea(14L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxNF0gUmVnbGEgZGUgMzAgY20="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("2.00"),1,"GRAVADO")),
        Map.entry(15L,new Linea(15L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxNV0gQ29tcMOhcyBlc2NvbGFy"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("8.00"),1,"EXONERADO")),
        Map.entry(16L,new Linea(16L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxNl0gVHJhbnNwb3J0YWRvciAxODDCsA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("2.50"),1,"GRAVADO")),
        Map.entry(17L,new Linea(17L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxN10gQ29sb3JlcyBkZSAxMiB1bmlkYWRlcw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("10.00"),1,"GRAVADO")),
        Map.entry(18L,new Linea(18L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxOF0gQ29sb3JlcyBkZSAyNCB1bmlkYWRlcw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("19.00"),1,"GRAVADO")),
        Map.entry(19L,new Linea(19L,new String(Base64.getDecoder().decode("UFJVRUJBIFsxOV0gUGx1bW9uZXMgZGUgMTIgY29sb3Jlcw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("12.00"),1,"GRAVADO")),
        Map.entry(20L,new Linea(20L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyMF0gQ29ycmVjdG9yIGzDrXF1aWRv"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.50"),1,"GRAVADO")),
        Map.entry(21L,new Linea(21L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyMV0gUmVzYWx0YWRvciBhbWFyaWxsbw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.00"),1,"GRAVADO")),
        Map.entry(22L,new Linea(22L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyMl0gUGx1bW9uZXMgZGUgMjQgY29sb3Jlcw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("22.00"),1,"GRAVADO")),
        Map.entry(23L,new Linea(23L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyM10gUmVzYWx0YWRvciByb3NhZG8="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.00"),1,"GRAVADO")),
        Map.entry(24L,new Linea(24L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyNF0gQ29ycmVjdG9yIGVuIGNpbnRh"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("5.00"),1,"EXONERADO")),
        Map.entry(25L,new Linea(25L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyNV0gR29tYSBlbiBiYXJyYQ=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("4.00"),1,"GRAVADO")),
        Map.entry(26L,new Linea(26L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyNl0gR29tYSBsw61xdWlkYQ=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("5.50"),1,"GRAVADO")),
        Map.entry(27L,new Linea(27L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyN10gU2lsaWNvbmEgbMOtcXVpZGE="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("4.50"),1,"GRAVADO")),
        Map.entry(28L,new Linea(28L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyOF0gVGlqZXJhIGVzY29sYXI="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("5.00"),1,"GRAVADO")),
        Map.entry(29L,new Linea(29L,new String(Base64.getDecoder().decode("UFJVRUJBIFsyOV0gQ2ludGEgYWRoZXNpdmEgdHJhbnNwYXJlbnRl"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("2.50"),1,"GRAVADO")),
        Map.entry(30L,new Linea(30L,new String(Base64.getDecoder().decode("UFJVRUJBIFszMF0gQ2ludGEgbWFza2luZyB0YXBl"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("4.50"),1,"EXONERADO")),
        Map.entry(31L,new Linea(31L,new String(Base64.getDecoder().decode("UFJVRUJBIFszMV0gUGFwZWwgYm9uZCBBMw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("18.00"),1,"GRAVADO")),
        Map.entry(32L,new Linea(32L,new String(Base64.getDecoder().decode("UFJVRUJBIFszMl0gUGFwZWwgYm9uZCBBNA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("22.00"),1,"GRAVADO")),
        Map.entry(33L,new Linea(33L,new String(Base64.getDecoder().decode("UFJVRUJBIFszM10gQ2FydHVsaW5hIGJsYW5jYQ=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(34L,new Linea(34L,new String(Base64.getDecoder().decode("UFJVRUJBIFszNF0gUGFwZWwgY3JlcMOp"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("2.50"),1,"GRAVADO")),
        Map.entry(35L,new Linea(35L,new String(Base64.getDecoder().decode("UFJVRUJBIFszNV0gQ2FydHVsaW5hIGRlIGNvbG9yZXM="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.50"),1,"GRAVADO")),
        Map.entry(36L,new Linea(36L,new String(Base64.getDecoder().decode("UFJVRUJBIFszNl0gUGFwZWwgbHVzdHJl"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.50"),1,"GRAVADO")),
        Map.entry(37L,new Linea(37L,new String(Base64.getDecoder().decode("UFJVRUJBIFszN10gRm9sZGVyIG1hbmlsYSBBNA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.00"),1,"GRAVADO")),
        Map.entry(38L,new Linea(38L,new String(Base64.getDecoder().decode("UFJVRUJBIFszOF0gRm9sZGVyIHBsw6FzdGljbyBBNA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.50"),1,"GRAVADO")),
        Map.entry(39L,new Linea(39L,new String(Base64.getDecoder().decode("UFJVRUJBIFszOV0gQXJjaGl2YWRvciBkZSBwYWxhbmNh"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("12.00"),1,"GRAVADO")),
        Map.entry(40L,new Linea(40L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0MF0gU29icmUgbWFuaWxhIEE0"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("1.00"),1,"GRAVADO")),
        Map.entry(41L,new Linea(41L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0MV0gR3JhcGFkb3JhIHBlcXVlw7Fh"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("12.00"),1,"GRAVADO")),
        Map.entry(42L,new Linea(42L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0Ml0gQ2FqYSBkZSBncmFwYXM="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.50"),1,"GRAVADO")),
        Map.entry(43L,new Linea(43L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0M10gUGVyZm9yYWRvciBkZSBwYXBlbA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("15.00"),1,"GRAVADO")),
        Map.entry(44L,new Linea(44L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0NF0gTm90YXMgYWRoZXNpdmFz"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("5.50"),1,"GRAVADO")),
        Map.entry(45L,new Linea(45L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0NV0gQ2xpcHMgbWV0w6FsaWNvcw=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.00"),1,"GRAVADO")),
        Map.entry(46L,new Linea(46L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0Nl0gU2VwYXJhZG9yZXMgYWRoZXNpdm9z"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("4.50"),1,"GRAVADO")),
        Map.entry(47L,new Linea(47L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0N10gUGx1bcOzbiBwYXJhIHBpemFycmEgYXp1bA=="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("4.50"),1,"GRAVADO")),
        Map.entry(48L,new Linea(48L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0OF0gUGx1bcOzbiBwYXJhIHBpemFycmEgbmVncm8="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("4.50"),1,"GRAVADO")),
        Map.entry(49L,new Linea(49L,new String(Base64.getDecoder().decode("UFJVRUJBIFs0OV0gQ2FsY3VsYWRvcmEgY2llbnTDrWZpY2E="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("45.00"),1,"GRAVADO")),
        Map.entry(50L,new Linea(50L,new String(Base64.getDecoder().decode("UFJVRUJBIFs1MF0gUGl6YXJyYSBhY3LDrWxpY2EgcGVxdWXDsWE="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("28.00"),1,"GRAVADO")),
        Map.entry(51L,new Linea(51L,new String(Base64.getDecoder().decode("UFJVRUJBIFs1MV0gcGluY2Vs"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.00"),1,"EXONERADO")),
        Map.entry(53L,new Linea(53L,new String(Base64.getDecoder().decode("UFJVRUJBIFs1M10gbHVja3k="),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("23.00"),1,"EXONERADO")),
        Map.entry(54L,new Linea(54L,new String(Base64.getDecoder().decode("UFJVRUJBIFs1NF0gd3d3"),java.nio.charset.StandardCharsets.UTF_8),new BigDecimal("3.00"),1,"GRAVADO"))
// FIN COPIA AUTORIZADA
    );
    public static List<Linea> lineas(int a, int b, int c) {
        if (a < 0 || a > 10 || b < 0 || b > 10 || c < 0 || c > 10 || a+b+c == 0)
            throw new IllegalArgumentException("Cantidades de prueba invalidas");
        List<Linea> r = new ArrayList<>();
        if (a>0) r.add(new Linea(900001L,"Cuaderno de prueba",new BigDecimal("11.80"),a,"GRAVADO"));
        if (b>0) r.add(new Linea(900002L,"Lapicero de prueba",new BigDecimal("5.90"),b,"GRAVADO"));
        if (c>0) r.add(new Linea(900003L,"Borrador de prueba",new BigDecimal("2.36"),c,"GRAVADO"));
        return List.copyOf(r);
    }
    public static List<Linea> lineas(String items, long centimos) {
        if (items == null || items.length()>2000 || !items.matches("[1-9][0-9]{0,14}:[1-9][0-9]?(,[1-9][0-9]{0,14}:[1-9][0-9]?)*"))
            throw new IllegalArgumentException("Carrito de prueba invalido");
        String[] partes=items.split(",");
        if(partes.length>50 || centimos<=0) throw new IllegalArgumentException("Limite de carrito de prueba");
        Set<Long> ids=new HashSet<>(); List<Linea> salida=new ArrayList<>(); BigDecimal total=BigDecimal.ZERO;
        for(String parte:partes){
            String[] x=parte.split(":"); long id=Long.parseLong(x[0]); int cantidad=Integer.parseInt(x[1]);
            Linea l=COPIA.get(id);
            if(l==null || cantidad>10 || !ids.add(id)) throw new IllegalArgumentException("Articulo no disponible en copia de prueba o cantidad invalida");
            salida.add(new Linea(id,l.nombre(),l.precio(),cantidad,l.igv()));
            total=total.add(l.precio().multiply(BigDecimal.valueOf(cantidad)));
        }
        if(total.compareTo(BigDecimal.valueOf(centimos,2))!=0) throw new IllegalArgumentException("Precio distinto de copia Beta; actualice catalogo autorizado");
        return List.copyOf(salida);
    }
    public static boolean valida(String nombre, BigDecimal precio, Integer cantidad, String igv) {
        if (precio == null || cantidad == null || cantidad < 1 || cantidad > 10 || igv==null) return false;
        return java.util.stream.Stream.concat(lineas(1,1,1).stream(),COPIA.values().stream())
            .anyMatch(x -> x.nombre().equals(nombre) && x.precio().compareTo(precio)==0 && x.igv().equals(igv));
    }
}
