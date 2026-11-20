package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;

public interface InstituicaoFinanceiraRepository extends JpaRepository<InstituicaoFinanceira, UUID> {

	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);
}
