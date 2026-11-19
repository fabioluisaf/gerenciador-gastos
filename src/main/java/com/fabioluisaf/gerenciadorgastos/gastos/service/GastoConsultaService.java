package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosPorIntervaloUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarPeriodosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FiltroGastos;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoSpecifications;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GastoConsultaService {

	private final GastoRepository gastoRepository;

	public List<PeriodoComGastosResponse> listarPeriodosRealizados(FiltrarPeriodosUrlParams filtros) {
		return gastoRepository.listarPeriodosRealizados(
				FiltroGastos.porMetodosECategorias(filtros.metodoPagamentoId(), filtros.categoria()));
	}

	public List<PeriodoComGastosResponse> listarPeriodosPlanejados(FiltrarPeriodosUrlParams filtros) {
		return gastoRepository.listarPeriodosPlanejados(
				FiltroGastos.porMetodosECategorias(filtros.metodoPagamentoId(), filtros.categoria()));
	}

	public Page<Gasto> listarRealizados(FiltrarGastosUrlParams filtros, Pageable pageable) {
		return listar(true, filtros, pageable);
	}

	public Page<Gasto> listarPlanejados(FiltrarGastosUrlParams filtros, Pageable pageable) {
		return listar(false, filtros, pageable);
	}

	private Page<Gasto> listar(boolean realizado, FiltrarGastosUrlParams filtros, Pageable pageable) {
		final var filtro = FiltroGastos.de(filtros.inicio(), filtros.fim(),
				filtros.metodoPagamentoId(), filtros.categoria(), filtros.faturaId());
		return gastoRepository.findAll(GastoSpecifications.listagem(realizado, filtro), pageable);
	}

	public Page<TotalCategoriaResponse> totalizarPorCategoria(
			BuscarGastosPorIntervaloUrlParams request, Pageable pageable) {
			
		return gastoRepository.totalizarPorCategoria(request.inicio(), request.fim(), pageable);
	}

	public Page<TotalMetodoPagamentoResponse> totalizarPorMetodoPagamento(
			BuscarGastosPorIntervaloUrlParams request, Pageable pageable) {
				
		return gastoRepository.totalizarPorMetodoPagamento(request.inicio(), request.fim(), pageable);
	}
}
