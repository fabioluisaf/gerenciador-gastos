package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ValoresParcelasInvalidosException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.CompraParceladaRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class CompraParceladaService {

	private final CompraParceladaRepository compraParceladaRepository;
	private final MetodoPagamentoService metodoPagamentoService;
	private final FaturaService faturaService;

	@Transactional
	public CompraParcelada criar(@NotNull @Valid CriarCompraParceladaRequest request) {
		final var compra = new CompraParcelada();
		final var metodo = buscarMetodoCredito(request.metodoPagamentoId());
		final var dataFechamentoInicial = resolverDataFechamentoInicial(
				metodo, request.data(), request.faturaDataFechamento()
		);
		compra.setValorTotal(request.valorTotal());

		gerarParcelas(compra, request.quantidadeParcelas());
		aplicarDados(compra, request.data(), request.nome(), request.categoria(),
				metodo, dataFechamentoInicial, request.descricao());

		return compraParceladaRepository.save(compra);
	}

	@Transactional
	public CompraParcelada atualizar(@NotNull UUID id, @NotNull @Valid AtualizarCompraParceladaRequest request) {
		final var compra = buscarEntidadePorId(id);
		validarValores(request);
		final var metodo = buscarMetodoCredito(request.metodoPagamentoId());
		final var dataFechamentoInicial = resolverDataFechamentoInicial(
				metodo, request.data(), request.faturaDataFechamento()
		);
		final var parcelas = compra.getParcelas();
		final var recriar = compra.getValorTotal().compareTo(request.valorTotal()) != 0
				|| parcelas.size() != request.quantidadeParcelas();

		if (recriar) {
			if (parcelas.stream().noneMatch(parcela -> Integer.valueOf(1).equals(parcela.getNumeroParcela()))) {
				throw new IllegalStateException("Compra parcelada sem primeira parcela: " + id);
			}

			parcelas.clear();
			compraParceladaRepository.flush();

			compra.setValorTotal(request.valorTotal());
			gerarParcelas(compra, request.quantidadeParcelas());
		}

		aplicarDados(compra, request.data(), request.nome(), request.categoria(),
				metodo, dataFechamentoInicial, request.descricao());
		aplicarValores(compra, request.valoresParcelas());

		return compraParceladaRepository.saveAndFlush(compra);
	}

	private MetodoPagamento buscarMetodoCredito(UUID metodoPagamentoId) {
		final var metodo = metodoPagamentoService.buscarEntidadePorId(metodoPagamentoId);
		if (metodo.getTipo() != TipoMetodoPagamento.CREDITO) {
			throw new PagamentoInvalidoException("COMPRA_PARCELADA_EXIGE_CREDITO");
		}

		return metodo;
	}

	@Transactional
	public void excluir(UUID id) {
		compraParceladaRepository.delete(buscarEntidadePorId(id));
	}

	@Transactional(readOnly = true)
	public CompraParcelada buscarPorId(UUID id) {
		return buscarEntidadePorId(id);
	}

	private CompraParcelada buscarEntidadePorId(UUID id) {
		return compraParceladaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Compra parcelada não encontrada: " + id));
	}
	private void validarValores(AtualizarCompraParceladaRequest request) {
		final var valores = request.valoresParcelas();
		if (valores == null) {
			return;
		}
		if (valores.size() != request.quantidadeParcelas()) {
			throw new ValoresParcelasInvalidosException("QUANTIDADE_VALORES_PARCELAS_INVALIDA");
		}
		var soma = BigDecimal.ZERO;
		for (final var valor : valores) {
			soma = soma.add(Objects.requireNonNull(valor));
		}
		if (soma.compareTo(request.valorTotal()) != 0) {
			throw new ValoresParcelasInvalidosException("SOMA_PARCELAS_DIFERENTE_DO_TOTAL");
		}
	}

	private void gerarParcelas(CompraParcelada compra, int quantidade) {
		final var valorParcela = compra.getValorTotal().divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);
		final var valorPrimeiraParcela = compra.getValorTotal()
				.subtract(valorParcela.multiply(BigDecimal.valueOf(quantidade - 1L)));
		for (int indice = 0; indice < quantidade; indice++) {
			final var parcela = new Gasto();
			parcela.setValor(indice == 0 ? valorPrimeiraParcela : valorParcela);
			parcela.setRealizado(true);
			parcela.setCompraParcelada(compra);
			parcela.setNumeroParcela(indice + 1);
			compra.getParcelas().add(parcela);
		}
	}

	private void aplicarDados(
			CompraParcelada compra, LocalDate dataInicial, String nome, String categoria,
			MetodoPagamento metodo, LocalDate dataFechamentoInicial, String descricao
	) {
		for (final var parcela : compra.getParcelas()) {
			final var indice = parcela.getNumeroParcela() - 1;
			final var fechamento = faturaService.dataFechamentoSeguinte(metodo, dataFechamentoInicial, indice);
			parcela.setData(indice == 0 ? dataInicial : dataInicial.withDayOfMonth(1).plusMonths(indice));
			parcela.setNome(nome);
			parcela.setCategoria(categoria);
			parcela.setMetodoPagamento(metodo);
			parcela.setFatura(faturaService.obterOuCriar(metodo.getId(), fechamento));
			parcela.setDescricao(descricao);
		}
	}

	private void aplicarValores(CompraParcelada compra, List<BigDecimal> valores) {
		if (valores == null) {
			return;
		}
		for (final var parcela : compra.getParcelas()) {
			parcela.setValor(valores.get(parcela.getNumeroParcela() - 1));
		}
	}

	private LocalDate resolverDataFechamentoInicial(
			MetodoPagamento metodo, LocalDate dataCompra, LocalDate dataFechamentoSelecionada
	) {
		final var dataFechamento = dataFechamentoSelecionada != null
				? dataFechamentoSelecionada
				: faturaService.sugerirDataFechamento(metodo, dataCompra);
		if (dataFechamento == null) {
			throw new PagamentoInvalidoException("FATURA_OBRIGATORIA_SEM_DATA_DA_COMPRA");
		}
		faturaService.validarDataFechamento(metodo, dataFechamento);

		return dataFechamento;
	}
}
