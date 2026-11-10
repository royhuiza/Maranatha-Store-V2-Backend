package com.libreria.producto.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    public static final int STOCK_MINIMO_POR_DEFECTO = 5;
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

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock;

    /** T40: umbral propio de cada artículo; por defecto 5 unidades. */
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Column(name = "stock_minimo", nullable = false, columnDefinition = "integer not null default 5")
    private Integer stockMinimo = STOCK_MINIMO_POR_DEFECTO;

    @Column(nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    private Long salidasPorVenta;

    @Version
    private Long version;

    public Producto() {}

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    /** T41: compara el stock actual con el stock mínimo del artículo. Se calcula, no se guarda. */
    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getEstadoStock() {
        int actual = stock == null ? 0 : stock;
        int minimo = stockMinimo == null ? STOCK_MINIMO_POR_DEFECTO : stockMinimo;
        if (actual <= 0) return "SIN_STOCK";
        return actual <= minimo ? "STOCK_BAJO" : "DISPONIBLE";
    }

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
}
