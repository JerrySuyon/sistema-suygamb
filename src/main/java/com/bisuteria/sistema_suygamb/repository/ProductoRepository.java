package com.bisuteria.sistema_suygamb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bisuteria.sistema_suygamb.model.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer>{


    // BUSQUEDA POR NOMBRE O CODIGO (si la necesitas)
    List<Producto> findByNombreContainingOrCodigoContaining(
            String nombre,
            String codigo);



    // BUSQUEDA CODIGO EXACTO
    Optional<Producto> findByCodigo(String codigo);



    // PRODUCTOS ACTIVOS
    List<Producto> findByActivoTrue();


}