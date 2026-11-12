package com.libreria.ventas.service;
import com.libreria.ventas.model.*;import java.time.*;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PeruTimeTest {
 @Test void utcTimestampCrossesBackToCorrectPeruDate(){assertEquals("2026-11-02T23:17:24-05:00",PeruTime.fromStored(LocalDateTime.parse("2026-11-03T04:17:24"),"UTC").toString());}
 @Test void preservesLegacyTimestampWithoutProvenTimezone(){assertEquals("2026-11-01T22:46:33-05:00",PeruTime.fromStored(LocalDateTime.parse("2026-11-01T22:46:33"),null).toString());}
 @Test void peruTimestampIsNotConvertedTwice(){assertEquals("2026-11-02T23:17:24-05:00",PeruTime.fromStored(LocalDateTime.parse("2026-11-02T23:17:24"),"America/Lima").toString());}
 @Test void saleSerializesOffset(){var v=new Venta();v.setFecha(LocalDateTime.parse("2026-11-03T00:33:42"));v.setFechaZona("UTC");assertEquals(LocalDate.of(2026,11,2),v.getFecha().toLocalDate());assertEquals(ZoneOffset.ofHours(-5),v.getFecha().getOffset());}
}
