package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, UUID>{

	Page<Gasto> findByDataBetween(LocalDate inicio, LocalDate fim, Pageable pageable);
}
