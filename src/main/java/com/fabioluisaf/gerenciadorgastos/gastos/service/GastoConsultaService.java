package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosPorIntervaloUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GastoConsultaService {

	private final GastoRepository gastoRepository;

	public List<PeriodoComGastosResponse> listarPeriodosComGastos() {
		return gastoRepository.listarPeriodosComGastos();
	}

	public Page<Gasto> listarPorIntervalo(
			BuscarGastosPorIntervaloUrlParams request, 
			FiltrarGastosUrlParams filtros, Pageable pageable) {

		return gastoRepository.listarPorIntervalo(
				request.inicio(), request.fim(), filtros.metodoPagamentoId(), filtros.categoria(), pageable);
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
