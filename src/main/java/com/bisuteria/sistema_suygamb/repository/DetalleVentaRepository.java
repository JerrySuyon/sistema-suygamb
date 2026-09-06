package com.bisuteria.sistema_suygamb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bisuteria.sistema_suygamb.model.DetalleVenta;

public interface DetalleVentaRepository
extends JpaRepository<DetalleVenta, Integer>{

    // TOP 5 PARA EL DASHBOARD

    @Query(value = """
            SELECT p.codigo,
                   p.nombre,
                   SUM(d.cantidad)
            FROM detalle_venta d
            INNER JOIN productos p
            ON p.id = d.producto_id
            GROUP BY p.codigo, p.nombre
            ORDER BY SUM(d.cantidad) DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> productosMasVendidos();

    // RANKING COMPLETO

    @Query(value = """
            SELECT p.codigo,
                   p.nombre,
                   SUM(d.cantidad)
            FROM detalle_venta d
            INNER JOIN productos p
            ON p.id = d.producto_id
            GROUP BY p.codigo, p.nombre
            ORDER BY SUM(d.cantidad) DESC
            """, nativeQuery = true)
    List<Object[]> obtenerProductosMasVendidos();

}