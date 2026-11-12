package com.libreria.ventas.model;
import java.time.*;
public final class PeruTime {
 public static final ZoneId ZONE=ZoneId.of("America/Lima");
 private PeruTime(){}
 public static OffsetDateTime fromStored(LocalDateTime date,String sourceZone){
  if(date==null)return null;
  ZoneId source=sourceZone==null?ZONE:ZoneId.of(sourceZone);
  return date.atZone(source).withZoneSameInstant(ZONE).toOffsetDateTime();
 }
}
