package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.TotalizarGastosRequest;
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
	private final FaturaService faturaService;

	public Page<Gasto> listarPorMesEAno(BuscarGastosRequest request, FiltrarGastosRequest filtros, Pageable pageable) {
		final var periodo = request.periodo();

		return gastoRepository.listarPorPeriodo(
            periodo.atDay(1), periodo.atEndOfMonth(),
            filtros.metodoPagamentoId(), filtros.categoria(), pageable
        );
	}

	public Page<Gasto> listarPorFatura(UUID faturaId, String categoria, Pageable pageable) {
		faturaService.validarExistencia(faturaId);

		return gastoRepository.listarPorFatura(faturaId, categoria, pageable);
	}

	public Page<TotalCategoriaResponse> totalizarPorCategoria(TotalizarGastosRequest request, Pageable pageable) {
		validarFaturaTotalizacao(request);

		return gastoRepository.totalizarPorCategoria(request.inicio(), request.fim(), request.faturaId(), pageable);
	}

	public Page<TotalMetodoPagamentoResponse> totalizarPorMetodoPagamento(
			TotalizarGastosRequest request, Pageable pageable
	) {
		validarFaturaTotalizacao(request);

		return gastoRepository.totalizarPorMetodoPagamento(request.inicio(), request.fim(), request.faturaId(), pageable);
	}

	private void validarFaturaTotalizacao(TotalizarGastosRequest request) {
		if (request.faturaId() != null) {
			faturaService.validarExistencia(request.faturaId());
		}
	}
}
