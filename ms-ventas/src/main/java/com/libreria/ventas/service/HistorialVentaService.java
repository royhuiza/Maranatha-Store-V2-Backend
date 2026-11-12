package com.libreria.ventas.service;

import com.libreria.ventas.dto.HistorialVentas;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.PeruTime;
import com.libreria.ventas.model.Venta;
import com.libreria.ventas.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** T67 (consulta filtrada) y T69 (rango de fechas + paginación) del historial de ventas. */
@Service
public class HistorialVentaService {

    public static final int TAMANO_MAXIMO = 100;
    private static final Pattern SERIE_NUMERO = Pattern.compile("^([A-Z]\\d{3})-?(\\d+)$");

    private final VentaRepository ventas;
    private final EntityManager em;

    public HistorialVentaService(VentaRepository ventas, EntityManager em) {
        this.ventas = ventas;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public HistorialVentas consultar(String desdeTexto, String hastaTexto, String numero, int pagina, int tamano) {
        LocalDate desde = fecha(desdeTexto, "desde");
        LocalDate hasta = fecha(hastaTexto, "hasta");
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página empieza en 0.");
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }

        Specification<Venta> filtro = filtro(desde, hasta, numero);
        var orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        var resultado = ventas.findAll(filtro, PageRequest.of(pagina, tamano, orden));
        // Solo se cargan los detalles de las ventas de esta página.
        resultado.getContent().forEach(v -> Hibernate.initialize(v.getDetalles()));
        return new HistorialVentas(resultado.getContent(), pagina, tamano, resultado.getTotalElements(),
                resultado.getTotalPages(), sumarEmitidas(filtro), ventas.count());
    }

    /** Fechas en hora de Perú (así se guardan): desde las 00:00 del día "desde" hasta el final del día "hasta". */
    static Specification<Venta> filtro(LocalDate desde, LocalDate hasta, String numero) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (desde != null || hasta != null) {
                // Algunas ventas antiguas se guardaron en UTC (fecha_zona = 'UTC'); el rango se traduce para cada caso.
                var fecha = root.<LocalDateTime>get("fecha");
                var zona = root.<String>get("fechaZona");
                Predicate enUtc = zona.in(ZONAS_UTC);
                // Predicados separados: en Hibernate 6, cb.not() puede invertir el mismo objeto.
                Predicate enPeru = cb.or(cb.isNull(zona), cb.not(zona.in(ZONAS_UTC)));
                LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
                LocalDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
                condiciones.add(cb.or(
                        cb.and(enPeru, rango(cb, fecha, inicio, fin)),
                        cb.and(enUtc, rango(cb, fecha, aUtc(inicio), aUtc(fin)))));
            }

            String texto = textoNumero(numero);
            if (!texto.isEmpty()) {
                var m = SERIE_NUMERO.matcher(texto);
                if (texto.matches("\\d{1,12}")) {
                    // "12" o "00000012": ese número en cualquier serie.
                    condiciones.add(cb.equal(root.get("numero"), correlativo(texto)));
                } else if (m.matches() && m.group(2).length() <= 12
                        && (!m.group(2).startsWith("0") || m.group(2).length() == 8)) {
                    // "B001-12" o "B001-00000012": comprobante exacto.
                    condiciones.add(cb.equal(root.get("serie"), m.group(1)));
                    condiciones.add(cb.equal(root.get("numero"), correlativo(m.group(2))));
                } else {
                    // Texto parcial: "B001", "F001-0000", etc.
                    condiciones.add(cb.like(cb.concat(cb.concat(root.get("serie"), "-"), root.get("numero")), "%" + texto + "%"));
                }
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static final List<String> ZONAS_UTC = List.of("UTC", "Etc/UTC", "Z", "GMT");

    private static Predicate rango(jakarta.persistence.criteria.CriteriaBuilder cb,
                                   jakarta.persistence.criteria.Expression<LocalDateTime> fecha,
                                   LocalDateTime inicio, LocalDateTime fin) {
        List<Predicate> partes = new ArrayList<>();
        if (inicio != null) partes.add(cb.greaterThanOrEqualTo(fecha, inicio));
        if (fin != null) partes.add(cb.lessThan(fecha, fin));
        return cb.and(partes.toArray(Predicate[]::new));
    }

    /** Hora de Perú → misma hora expresada en UTC (Perú no tiene horario de verano: +5 h). */
    static LocalDateTime aUtc(LocalDateTime horaPeru) {
        return horaPeru == null ? null
                : horaPeru.atZone(PeruTime.ZONE).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
    }

    static String textoNumero(String numero) {
        // Sin espacios ni comodines de LIKE escritos por el usuario.
        return numero == null ? "" : numero.toUpperCase(Locale.ROOT).replaceAll("[\\s%_\\\\]+", "");
    }

    static String correlativo(String digitos) {
        return String.format("%08d", Long.parseLong(digitos));
    }

    private BigDecimal sumarEmitidas(Specification<Venta> filtro) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(BigDecimal.class);
        var root = query.from(Venta.class);
        Predicate condicion = filtro.toPredicate(root, query, cb);
        Predicate noAnulada = cb.notEqual(root.get("estado"), EstadoVenta.ANULADA);
        query.select(cb.coalesce(cb.sum(root.<BigDecimal>get("total")), BigDecimal.ZERO))
                .where(condicion == null ? noAnulada : cb.and(condicion, noAnulada));
        return em.createQuery(query).getSingleResult();
    }

    private static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha '" + campo + "' debe tener el formato aaaa-mm-dd.");
        }
    }
}
