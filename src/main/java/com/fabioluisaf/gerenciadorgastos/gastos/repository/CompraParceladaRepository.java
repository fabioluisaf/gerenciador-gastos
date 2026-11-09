package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;

public interface CompraParceladaRepository extends JpaRepository<CompraParcelada, UUID> {

	@Override
	@EntityGraph(attributePaths = "parcelas")
	Optional<CompraParcelada> findById(UUID id);
}
