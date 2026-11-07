package com.pedidos360.local.repository;

import com.pedidos360.local.models.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalRepository extends JpaRepository<Local, Long> {
    Optional<Local> findByNombreIgnoreCase(String nombre);
}
