package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.fatura.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaturaService {

	private final FaturaRepository faturaRepository;
	private final MetodoPagamentoService metodoPagamentoService;

	public Page<FaturaResumidaResponse> listarPorMetodoPagamento(UUID metodoPagamentoId, Pageable pageable) {
		metodoPagamentoService.buscarPorId(metodoPagamentoId);

		return faturaRepository.listarResumosPorMetodoPagamentoId(metodoPagamentoId, pageable);
	}

	public void validarExistencia(UUID id) {
		if (!faturaRepository.existsById(id)) {
			throw new EntityNotFoundException("Fatura não encontrada: " + id);
		}
	}

	@Transactional
	public Fatura obterOuCriar(UUID metodoPagamentoId, LocalDate dataFechamento) {
		final var metodo = metodoPagamentoService.buscarEntidadePorId(metodoPagamentoId);
		validarCartao(metodo);
		validarDataFechamento(metodo, dataFechamento);

		return buscarPorPeriodo(metodoPagamentoId, YearMonth.from(dataFechamento))
				.orElseGet(() -> criar(metodo, dataFechamento));
	}

	private Optional<Fatura> buscarPorPeriodo(UUID metodoPagamentoId, YearMonth periodo) {
		return faturaRepository.findByMetodoPagamentoIdAndDataFechamentoBetween(
				metodoPagamentoId, periodo.atDay(1), periodo.atEndOfMonth()
		);
	}

	public LocalDate sugerirDataFechamento(MetodoPagamento metodo, LocalDate dataCompra) {
		if (metodo.getTipo() != TipoMetodoPagamento.CREDITO || dataCompra == null) {
			return null;
		}

		final var fechamentoDoMes = dataFechamento(YearMonth.from(dataCompra), metodo.getDiaFechamento());

		return dataCompra.isBefore(fechamentoDoMes)
				? fechamentoDoMes
				: dataFechamento(YearMonth.from(dataCompra).plusMonths(1), metodo.getDiaFechamento());
	}

	public void validarCartao(MetodoPagamento metodo) {
		if (metodo.getTipo() != TipoMetodoPagamento.CREDITO) {
			throw new PagamentoInvalidoException("FATURA_PERMITIDA_APENAS_PARA_CREDITO");
		}
	}

	private Fatura criar(MetodoPagamento metodo, LocalDate dataFechamento) {
		final var fatura = new Fatura();
		fatura.setMetodoPagamento(metodo);
		fatura.setDataFechamento(dataFechamento);

		return faturaRepository.save(fatura);
	}

	public void validarDataFechamento(MetodoPagamento metodo, LocalDate dataFechamento) {
		if (dataFechamento == null || !dataFechamento.equals(
				dataFechamento(YearMonth.from(dataFechamento), metodo.getDiaFechamento()))) {
			throw new PagamentoInvalidoException("DATA_FECHAMENTO_FATURA_INVALIDA");
		}
	}

	public LocalDate dataFechamentoSeguinte(MetodoPagamento metodo, LocalDate dataFechamento, int quantidadeMeses) {
		return dataFechamento(
				YearMonth.from(dataFechamento).plusMonths(quantidadeMeses), metodo.getDiaFechamento()
		);
	}

	private LocalDate dataFechamento(YearMonth periodo, int diaFechamento) {
		return periodo.atDay(Math.min(diaFechamento, periodo.lengthOfMonth()));
	}
}
