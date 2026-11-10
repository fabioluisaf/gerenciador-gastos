package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
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
		metodoPagamentoService.buscarEntidadePorId(metodoPagamentoId);

		return faturaRepository.listarResumosPorMetodoPagamentoId(metodoPagamentoId, pageable);
	}

	public void validarExistencia(UUID id) {
		if (!faturaRepository.existsById(id)) {
			throw new EntityNotFoundException("Fatura não encontrada: " + id);
		}
	}

	@Transactional
	public Fatura obterOuCriar(UUID metodoPagamentoId, YearMonth periodo) {
		final var metodo = metodoPagamentoService.buscarCreditoPorId(
				metodoPagamentoId, "FATURA_PERMITIDA_APENAS_PARA_CREDITO"
		);
        
		if (periodo == null) {
			throw new PagamentoInvalidoException("PERIODO_FATURA_OBRIGATORIO");
		}

		return faturaRepository
            .findByMetodoPagamentoIdAndDataFechamentoBetween(metodoPagamentoId, periodo.atDay(1), periodo.atEndOfMonth())
            .orElseGet(() -> criar(metodo, dataFechamento(periodo, metodo.getDiaFechamento())));
	}

	public YearMonth sugerirPeriodo(MetodoPagamento metodo, LocalDate dataCompra) {
		if (metodo.getTipo() != TipoMetodoPagamento.CREDITO || dataCompra == null) {
			return null;
		}

		final var periodo = YearMonth.from(dataCompra);
		final var fechamentoDoMes = dataFechamento(periodo, metodo.getDiaFechamento());

		return dataCompra.isBefore(fechamentoDoMes)
				? periodo
				: periodo.plusMonths(1);
	}

	private Fatura criar(MetodoPagamento metodo, LocalDate dataFechamento) {
		final var fatura = new Fatura();
		fatura.setMetodoPagamento(metodo);
		fatura.setDataFechamento(dataFechamento);

		return faturaRepository.save(fatura);
	}

	private LocalDate dataFechamento(YearMonth periodo, int diaFechamento) {
		return periodo.atDay(Math.min(diaFechamento, periodo.lengthOfMonth()));
	}
}
