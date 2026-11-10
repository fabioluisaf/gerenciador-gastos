package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ValoresParcelasInvalidosException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Parcelamento {

	private final FaturaService faturaService;

	public void validarValores(AtualizarCompraParceladaRequest request) {
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

	public void gerarParcelas(CompraParcelada compra, int quantidade) {
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

	public void aplicarDados(
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

	public void aplicarValores(CompraParcelada compra, List<BigDecimal> valores) {
		if (valores == null) {
			return;
		}
		for (final var parcela : compra.getParcelas()) {
			parcela.setValor(valores.get(parcela.getNumeroParcela() - 1));
		}
	}

	public LocalDate resolverDataFechamentoInicial(
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
