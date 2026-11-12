package com.libreria.producto.service;

import com.libreria.producto.model.Producto;
import com.libreria.producto.repository.ProductoRepository;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ImportacionProductos {
    private static final int MAX_FILAS = 2000;
    private static final int MAX_ERRORES_VISIBLES = 10;
    private final ProductoRepository productos;
    private final JdbcTemplate jdbc;
    private final Validator validator;

    public ImportacionProductos(ProductoRepository productos, JdbcTemplate jdbc, Validator validator) {
        this.productos = productos;
        this.jdbc = jdbc;
        this.validator = validator;
    }

    @Transactional
    public Resultado importar(MultipartFile archivo) {
        List<Fila> filas = leer(archivo);

        // Serializa importaciones para impedir duplicados entre dos cargas del mismo archivo.
        jdbc.query("select pg_advisory_xact_lock(83910517)", rs -> { });
        Map<String, List<Long>> porNombre = new HashMap<>();
        Set<Long> idsExistentes = new HashSet<>();
        Map<Long, String> nombrePorId = new HashMap<>();
        // Carga solo las referencias para comparar después contra cada fila bloqueada.
        jdbc.query("select id, nombre from productos", resultado -> {
            long id = resultado.getLong("id");
            idsExistentes.add(id);
            nombrePorId.put(id, clave(resultado.getString("nombre")));
            porNombre.computeIfAbsent(clave(resultado.getString("nombre")), k -> new ArrayList<>()).add(id);
        });

        List<Producto> nuevos = new ArrayList<>();
        Set<String> nombresNuevos = new HashSet<>();
        Map<Long, Fila> cambios = new TreeMap<>();
        for (Fila fila : filas) {
            Producto producto = fila.producto();
            Long id = producto.getId();
            String nombre = clave(producto.getNombre());
            if (id != null && !idsExistentes.contains(id)) throw error(fila.numero(), "el ID " + id + " no existe");
            final Long idFila = id;
            // T06: renombrar por ID a un nombre que ya usa otro artículo genera duplicados.
            if (id != null && !nombre.equals(nombrePorId.get(id))
                    && porNombre.getOrDefault(nombre, List.of()).stream().anyMatch(otro -> !otro.equals(idFila))) {
                throw error(fila.numero(), "ya existe otro artículo llamado \"" + producto.getNombre().strip() + "\"");
            }
            if (id == null) {
                var coincidencias = porNombre.getOrDefault(nombre, List.of());
                if (coincidencias.size() > 1) throw error(fila.numero(), "hay varios artículos con este nombre. Indica el ID");
                if (coincidencias.size() == 1) id = coincidencias.get(0);
            }
            if (id == null) {
                if (!nombresNuevos.add(nombre)) throw error(fila.numero(), "el nombre está repetido en el archivo");
                nuevos.add(producto);
            } else if (cambios.putIfAbsent(id, fila) != null) {
                throw error(fila.numero(), "el artículo con ID " + id + " está repetido en el archivo");
            }
        }

        int actualizados = 0;
        for (var cambio : cambios.entrySet()) {
            Fila fila = cambio.getValue();
            Producto actual = productos.findByIdForUpdate(cambio.getKey())
                    .orElseThrow(() -> error(fila.numero(), "el producto ya no existe"));
            Producto datos = fila.producto();
            if (datos.getId() == null && !clave(actual.getNombre()).equals(clave(datos.getNombre()))) {
                throw error(fila.numero(), "el nombre del artículo cambió durante la importación. Indica su ID");
            }
            if (mismosDatos(actual, datos)) continue;

            actualizados++;
            actual.setNombre(datos.getNombre());
            actual.setDescripcion(datos.getDescripcion());
            actual.setPrecio(datos.getPrecio());
            actual.setStock(datos.getStock());
            if (datos.getActivo() != null) actual.setActivo(datos.getActivo());
        }
        if (!nuevos.isEmpty()) {
            jdbc.batchUpdate("""
                    insert into productos (nombre, descripcion, precio, stock, activo, version)
                    values (?, ?, ?, ?, ?, 0)
                    """, nuevos, 100, (ps, producto) -> {
                ps.setString(1, producto.getNombre());
                ps.setString(2, producto.getDescripcion());
                ps.setBigDecimal(3, producto.getPrecio());
                ps.setInt(4, producto.getStock());
                ps.setBoolean(5, !Boolean.FALSE.equals(producto.getActivo()));
            });
        }
        return new Resultado(nuevos.size(), actualizados, cambios.size() - actualizados);
    }

    private boolean mismosDatos(Producto actual, Producto datos) {
        return Objects.equals(actual.getNombre(), datos.getNombre())
                && Objects.equals(actual.getDescripcion(), datos.getDescripcion())
                && actual.getPrecio() != null && actual.getPrecio().compareTo(datos.getPrecio()) == 0
                && Objects.equals(actual.getStock(), datos.getStock())
                && (datos.getActivo() == null || Objects.equals(actual.getActivo(), datos.getActivo()));
    }

    private List<Fila> leer(MultipartFile archivo) {
        String nombre = Objects.toString(archivo.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (archivo.isEmpty() || !(nombre.endsWith(".xlsx") || nombre.endsWith(".xls"))) {
            throw error(0, "selecciona un archivo Excel (.xlsx o .xls) con datos");
        }
        try (var entrada = archivo.getInputStream(); var libro = WorkbookFactory.create(entrada)) {
            if (libro.getNumberOfSheets() == 0) throw error(0, "el archivo no tiene hojas");
            var hoja = libro.getSheetAt(0);
            var formato = new DataFormatter(Locale.ROOT);
            formato.setUseCachedValuesForFormulaCells(true);
            Map<String, Integer> columnas = encabezados(hoja.getRow(0), formato);
            if (hoja.getLastRowNum() > MAX_FILAS) throw error(0, "se permiten hasta " + MAX_FILAS + " filas de datos");
            List<Fila> filas = new ArrayList<>();
            List<String> errores = new ArrayList<>();
            int filasInvalidas = 0;
            for (Row fila : hoja) {
                if (fila.getRowNum() == 0 || columnas.values().stream().allMatch(i -> texto(fila, i, formato).isBlank())) continue;
                try {
                    filas.add(leerFila(fila, columnas, formato));
                } catch (ResponseStatusException ex) {
                    filasInvalidas++;
                    if (errores.size() < MAX_ERRORES_VISIBLES) errores.add(ex.getReason());
                }
            }
            if (filasInvalidas > 0) {
                if (filasInvalidas > errores.size()) {
                    errores.add("Hay " + (filasInvalidas - errores.size()) + " filas adicionales con errores.");
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se guardaron cambios. Corrige el Excel y vuelve a importarlo.\n" + String.join("\n", errores));
            }
            if (filas.isEmpty()) throw error(0, "el archivo no contiene productos");
            return filas;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo Excel.", ex);
        }
    }

    private Map<String, Integer> encabezados(Row cabecera, DataFormatter formato) {
        Map<String, Integer> columnas = new HashMap<>();
        if (cabecera != null) {
            for (Cell celda : cabecera) {
                String clave = switch (clave(formato.formatCellValue(celda))) {
                    case "id" -> "id";
                    case "nombre", "producto", "product" -> "nombre";
                    case "descripción", "descripcion", "desc" -> "descripcion";
                    case "precio", "price" -> "precio";
                    case "stock", "cantidad", "qty" -> "stock";
                    case "activo", "estado", "enabled" -> "activo";
                    default -> null;
                };
                if (clave != null && columnas.putIfAbsent(clave, celda.getColumnIndex()) != null) {
                    throw error(1, "el encabezado " + clave + " aparece más de una vez");
                }
            }
        }
        if (!columnas.keySet().containsAll(List.of("nombre", "descripcion", "precio", "stock"))) {
            throw error(1, "se requieren los encabezados Nombre, Descripción, Precio y Stock");
        }
        return columnas;
    }

    private Fila leerFila(Row fila, Map<String, Integer> columnas, DataFormatter formato) {
        int numero = fila.getRowNum() + 1;
        Producto producto = new Producto();
        producto.setNombre(texto(fila, columnas.get("nombre"), formato));
        producto.setDescripcion(texto(fila, columnas.get("descripcion"), formato));
        try {
            if (!texto(fila, columnas.get("precio"), formato).isBlank()) {
                producto.setPrecio(decimal(fila, columnas.get("precio"), formato).setScale(2));
            }
            if (!texto(fila, columnas.get("stock"), formato).isBlank()) {
                producto.setStock(decimal(fila, columnas.get("stock"), formato).intValueExact());
            }
            if (!texto(fila, columnas.get("id"), formato).isBlank()) {
                long id = decimal(fila, columnas.get("id"), formato).longValueExact();
                if (id < 1) throw error(numero, "el ID debe ser positivo");
                producto.setId(id);
            }
        } catch (ArithmeticException | NumberFormatException ex) {
            throw error(numero, "revisa el ID, el precio (hasta dos decimales) y el stock (entero)");
        }
        producto.setActivo(switch (clave(texto(fila, columnas.get("activo"), formato))) {
            case "" -> null;
            case "si", "sí", "yes", "true", "1", "activo", "enabled" -> true;
            case "no", "false", "0", "inactivo", "disabled" -> false;
            default -> throw error(numero, "Activo debe ser Sí o No");
        });
        var errores = validator.validate(producto, Producto.DatosCatalogo.class);
        if (!errores.isEmpty()) {
            throw error(numero, String.join(" · ", errores.stream().map(e -> e.getMessage()).distinct().sorted().toList()));
        }
        return new Fila(numero, producto);
    }

    private BigDecimal decimal(Row fila, Integer columna, DataFormatter formato) {
        Cell celda = columna == null ? null : fila.getCell(columna);
        if (celda != null && (celda.getCellType() == CellType.NUMERIC
                || celda.getCellType() == CellType.FORMULA && celda.getCachedFormulaResultType() == CellType.NUMERIC)) {
            return BigDecimal.valueOf(celda.getNumericCellValue());
        }
        return new BigDecimal(texto(fila, columna, formato).replace(',', '.'));
    }

    private String texto(Row fila, Integer columna, DataFormatter formato) {
        return columna == null ? "" : formato.formatCellValue(fila.getCell(columna)).strip();
    }

    private String clave(String texto) {
        return texto.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException error(int fila, String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, (fila > 0 ? "Fila " + fila + ": " : "") + mensaje + ".");
    }

    private record Fila(int numero, Producto producto) {}
    public record Resultado(int nuevos, int actualizados, int sinCambios) {}
}
