package com.bisuteria.sistema_suygamb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bisuteria.sistema_suygamb.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer>{
	Optional<Cliente> findByDni(String dni);
}
