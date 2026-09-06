package com.bisuteria.sistema_suygamb.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.bisuteria.sistema_suygamb.model.Producto;
import com.bisuteria.sistema_suygamb.repository.ProductoRepository;

@RestController
@RequestMapping("/productos")
@CrossOrigin("*")

public class ProductoController {
	@Autowired
    private ProductoRepository repo;

    // LISTAR
    @GetMapping
    public List<Producto> listar(){

        return repo.findAll();

    }

    // GUARDAR
    @PostMapping
    public Producto guardar(@RequestBody Producto producto){

        return repo.save(producto);

    }
}
