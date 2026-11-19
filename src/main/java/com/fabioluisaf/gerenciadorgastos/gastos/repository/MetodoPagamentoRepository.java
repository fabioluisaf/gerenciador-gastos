package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;

import jakarta.persistence.LockModeType;

public interface MetodoPagamentoRepository extends JpaRepository<MetodoPagamento, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select metodo from MetodoPagamento metodo where metodo.id = :id")
	Optional<MetodoPagamento> findByIdForUpdate(UUID id);

	boolean existsByInstituicaoFinanceiraId(UUID instituicaoFinanceiraId);
}
