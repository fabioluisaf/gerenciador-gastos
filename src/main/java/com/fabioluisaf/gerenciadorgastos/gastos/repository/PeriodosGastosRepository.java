package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.util.List;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;

public interface PeriodosGastosRepository {

	List<PeriodoComGastosResponse> listarPeriodosRealizados(FiltroGastos filtro);

	List<PeriodoComGastosResponse> listarPeriodosPlanejados(FiltroGastos filtro);
}
