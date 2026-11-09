package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;

public interface MetodoPagamentoRepository extends JpaRepository<MetodoPagamento, UUID> {
}
