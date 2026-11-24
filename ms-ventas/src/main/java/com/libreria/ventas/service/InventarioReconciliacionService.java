package com.libreria.ventas.service;

import com.libreria.ventas.client.ProductoClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InventarioReconciliacionService {

    private final ProductoClient productoClient;

    private final InventarioVerificacionService verificacion;

    public InventarioReconciliacionService(
            ProductoClient productoClient,
            InventarioVerificacionService verificacion
    ) {

        this.productoClient = productoClient;
        this.verificacion = verificacion;
    }

    public boolean compensarDescuentos(
            Long operacionId,
            Map<Long, Integer> cantidades
    ) {

        boolean resultadoCompleto = true;

        for (Map.Entry<Long, Integer> entry :
                cantidades.entrySet()) {

            Long productoId = entry.getKey();

            int cantidad = entry.getValue();

            String claveDescuento =
                    "VENTA-" + operacionId
                            + "-" + productoId;

            String claveReposicion =
                    "COMPENSACION-" + operacionId
                            + "-" + productoId;

            try {

                boolean descontado =
                        verificacion.descuentoConfirmado(
                                productoId,
                                cantidad,
                                claveDescuento
                        );

                if (!descontado) {

                    // No podemos descartar un descuento
                    // que todavia se encuentre en transito.

                    resultadoCompleto = false;
                    continue;
                }

                boolean compensado =
                        verificacion.descuentoCompensado(
                                productoId,
                                cantidad,
                                claveDescuento
                        );

                if (compensado) {
                    continue;
                }

                productoClient.reponerStockIdempotente(
                        productoId,
                        cantidad,
                        claveReposicion,
                        claveDescuento
                );

                boolean confirmado =
                        verificacion.descuentoCompensado(
                                productoId,
                                cantidad,
                                claveDescuento
                        );

                if (!confirmado) {
                    resultadoCompleto = false;
                }

            } catch (Exception ex) {

                resultadoCompleto = false;
            }
        }

        return resultadoCompleto;
    }
}
