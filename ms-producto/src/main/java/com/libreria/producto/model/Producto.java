package com.libreria.producto.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.groups.Default;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {
    public interface DatosCatalogo extends Default {}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre admite hasta 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
    @NotBlank(groups = DatosCatalogo.class, message = "La descripción es obligatoria")
    @Column(length = 500)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", groups = DatosCatalogo.class, message = "El precio debe ser mayor que cero (mínimo S/ 0.01)")
    @Digits(integer = 10, fraction = 2, groups = DatosCatalogo.class, message = "El precio admite hasta dos decimales")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "afectacion_igv", length = 20)
    private TipoAfectacionIgv afectacionIgv;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    private Long salidasPorVenta;

    @Version
    private Long version;

    public Producto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    @JsonIgnore
    public long salidasPorVenta() { return salidasPorVenta == null ? 0 : salidasPorVenta; }

    public void registrarSalidaVenta() { salidasPorVenta = salidasPorVenta() + 1; }

    public void revertirSalidaVenta() { salidasPorVenta = Math.max(0, salidasPorVenta() - 1); }

    public Long getVersion() { return version; }

    public TipoAfectacionIgv getAfectacionIgv() {
        return afectacionIgv;
    }

    public void setAfectacionIgv(TipoAfectacionIgv afectacionIgv) {
        this.afectacionIgv = afectacionIgv;
    }
}
