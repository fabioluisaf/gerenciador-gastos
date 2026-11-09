package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, UUID>{

	@EntityGraph(attributePaths = {"compraParcelada", "metodoPagamento", "fatura"})
	Page<Gasto> findByDataBetween(LocalDate inicio, LocalDate fim, Pageable pageable);

	@EntityGraph(attributePaths = { "metodoPagamento" })
	Page<Gasto> findByFaturaId(UUID faturaId, Pageable pageable);

	boolean existsByMetodoPagamentoId(UUID metodoPagamentoId);

	@Override
	@EntityGraph(attributePaths = {"compraParcelada", "metodoPagamento", "fatura"})
	Optional<Gasto> findById(UUID id);
}
