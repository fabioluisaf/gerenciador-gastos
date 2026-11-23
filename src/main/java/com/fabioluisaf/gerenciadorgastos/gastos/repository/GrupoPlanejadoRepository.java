package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fabioluisaf.gerenciadorgastos.gastos.model.GrupoPlanejado;

public interface GrupoPlanejadoRepository extends JpaRepository<GrupoPlanejado, UUID> {
}
