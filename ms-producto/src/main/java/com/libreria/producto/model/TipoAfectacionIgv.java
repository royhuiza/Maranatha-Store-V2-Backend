package com.libreria.producto.model;

public enum TipoAfectacionIgv {
    GRAVADO("10"),
    EXONERADO("20"),
    INAFECTO("30");

    private final String codigoSunat;

    TipoAfectacionIgv(String codigoSunat) {
        this.codigoSunat = codigoSunat;
    }

    public String getCodigoSunat() {
        return codigoSunat;
    }
}
