package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.BuscarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.FiltrarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.TotalizarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.GastoRealizadoSemDataException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
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
		aplicarMetodoPagamento(
				entidade, request.metodoPagamentoId(), request.data(), request.faturaDataFechamento()
		);

		return gastoRepository.save(entidade);
	}

	public Page<Gasto> listarPorMesEAno(BuscarGastosRequest request, FiltrarGastosRequest filtros, Pageable pageable) {
		final var periodo = YearMonth.of(request.ano(), request.mes());

		return gastoRepository.listarPorPeriodo(periodo.atDay(1), periodo.atEndOfMonth(),
				filtros.metodoPagamentoId(), filtros.categoria(), pageable);
	}

	public Gasto buscarPorId(UUID id) {
		return buscarEntidadePorId(id);
	}

	public Page<Gasto> listarPorFatura(UUID faturaId, FiltrarGastosRequest filtros, Pageable pageable) {
		faturaService.validarExistencia(faturaId);

		return gastoRepository.listarPorFatura(faturaId, filtros.metodoPagamentoId(), filtros.categoria(), pageable);
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

	@Transactional
	public Gasto atualizar(UUID id, AtualizarGastoRequest request) {
		final var entidade = buscarEntidadePorId(id);

		validarGastoAvulso(entidade);

		aplicarMetodoPagamento(
				entidade, request.metodoPagamentoId(), request.data(), request.faturaDataFechamento()
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
		final var entidade = buscarEntidadePorId(id);

		if (realizado && entidade.getData() == null) {
			throw new GastoRealizadoSemDataException();
		}
		entidade.setRealizado(realizado);

		return gastoRepository.save(entidade);
	}

	@Transactional
	public void excluir(UUID id) {
		final var gasto = buscarEntidadePorId(id);
		validarGastoAvulso(gasto);
		gastoRepository.delete(gasto);
	}

	private void validarGastoAvulso(Gasto gasto) {
		if (gasto.getCompraParcelada() != null) {
			throw new ParcelaOperacaoInvalidaException();
		}
	}

	private void aplicarMetodoPagamento(
			Gasto gasto, UUID metodoPagamentoId, LocalDate data, LocalDate faturaDataFechamento
	) {
		final var metodo = metodoPagamentoService.buscarEntidadePorId(metodoPagamentoId);
		gasto.setMetodoPagamento(metodo);

		if (metodo.getTipo() != TipoMetodoPagamento.CREDITO) {
			if (faturaDataFechamento != null) {
				throw new PagamentoInvalidoException("FATURA_PERMITIDA_APENAS_PARA_CREDITO");
			}

			gasto.setFatura(null);

			return;
		}

		final var dataFechamento = faturaDataFechamento != null
				? faturaDataFechamento
				: faturaService.sugerirDataFechamento(metodo, data);
		if (dataFechamento == null) {
			throw new PagamentoInvalidoException("FATURA_OBRIGATORIA_SEM_DATA_DA_COMPRA");
		}

		gasto.setFatura(faturaService.obterOuCriar(metodoPagamentoId, dataFechamento));
	}

	private Gasto buscarEntidadePorId(UUID id) {
		return gastoRepository
                .findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Gasto não encontrado: " + id));
	}

}
