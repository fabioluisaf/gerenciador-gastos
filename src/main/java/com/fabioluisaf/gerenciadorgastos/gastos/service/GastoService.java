package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.GastoRealizadoSemDataException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GastoService {

	private final GastoRepository gastoRepository;
	private final MetodoPagamentoService metodoPagamentoService;
	private final FaturaService faturaService;

	@Transactional
	public Gasto criar(CriarGastoRequest request) {
		final var entidade = request.toEntity();

		aplicarMetodoPagamentoEFatura(
            entidade, request.metodoPagamentoId(), request.data(), request.faturaPeriodo()
		);

		return gastoRepository.save(entidade);
	}

	public Gasto buscarPorId(UUID id) {
		return gastoRepository
				.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Gasto não encontrado: " + id));
	}

	@Transactional
	public Gasto atualizar(UUID id, AtualizarGastoRequest request) {
		final var entidade = buscarPorId(id);

		validarGastoAvulso(entidade);

		aplicarMetodoPagamentoEFatura(
				entidade, request.metodoPagamentoId(), request.data(), request.faturaPeriodo()
		);
		entidade.setData(request.data());
		entidade.setNome(request.nome());
		entidade.setValor(request.valor());
		entidade.setCategoria(request.categoria());
		entidade.setDescricao(request.descricao());
		entidade.setRealizado(request.realizado());

		return gastoRepository.save(entidade);
	}

	@Transactional
	public Gasto atualizarRealizado(UUID id, boolean realizado) {
		final var entidade = buscarPorId(id);

		if (realizado && entidade.getData() == null) {
			throw new GastoRealizadoSemDataException();
		}
		entidade.setRealizado(realizado);

		return gastoRepository.save(entidade);
	}

	@Transactional
	public void excluir(UUID id) {
		final var gasto = buscarPorId(id);
		validarGastoAvulso(gasto);
		gastoRepository.delete(gasto);
	}

	private void validarGastoAvulso(Gasto gasto) {
		if (gasto.getCompraParcelada() != null) {
			throw new ParcelaOperacaoInvalidaException();
		}
	}

	private void aplicarMetodoPagamentoEFatura(
        Gasto gasto, UUID metodoPagamentoId, LocalDate data, YearMonth faturaPeriodo
	) {
		final var metodo = metodoPagamentoService.buscarEntidadePorId(metodoPagamentoId);
		gasto.setMetodoPagamento(metodo);

		final var ehCredito = metodo.getTipo() == TipoMetodoPagamento.CREDITO;

		if (!ehCredito && faturaPeriodo != null) {
			throw new PagamentoInvalidoException("FATURA_PERMITIDA_APENAS_PARA_CREDITO");
		}

		if (!ehCredito) {
			gasto.setFatura(null);

			return;
		}

		final var periodo = faturaPeriodo != null
				? faturaPeriodo
				: faturaService.sugerirPeriodo(metodo, data);

		if (periodo == null) {
			throw new PagamentoInvalidoException("FATURA_OBRIGATORIA_SEM_DATA_DA_COMPRA");
		}

		gasto.setFatura(faturaService.obterOuCriar(metodoPagamentoId, periodo));
	}
}
