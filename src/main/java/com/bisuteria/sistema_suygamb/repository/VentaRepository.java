package com.bisuteria.sistema_suygamb.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.bisuteria.sistema_suygamb.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Integer>{
	
    @Query("""
            SELECT v.producto, SUM(v.cantidad)
            FROM Venta v
            GROUP BY v.producto
            ORDER BY SUM(v.cantidad) DESC
            """)
    List<Object[]> productosMasVendidos();

    // NUEVO - PARA HISTORIAL SIN QUE SE ROMPA
    @Query("SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto ORDER BY v.id ASC")
    List<Venta> findAllConDetalles();
}