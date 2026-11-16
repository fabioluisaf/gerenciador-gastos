package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ValoresParcelasInvalidosException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParcelamentoComponent {

	private final FaturaService faturaService;

	public void validarValores(BigDecimal valorTotal, int quantidadeParcelas, List<BigDecimal> valores) {
		if (valores == null) {
			return;
		}

		if (valores.size() != quantidadeParcelas) {
			throw new ValoresParcelasInvalidosException("QUANTIDADE_VALORES_PARCELAS_INVALIDA");
		}

		var soma = BigDecimal.ZERO;

		for (final var valor : valores) {
			soma = soma.add(Objects.requireNonNull(valor));
		}

		if (soma.compareTo(valorTotal) != 0) {
			throw new ValoresParcelasInvalidosException("SOMA_PARCELAS_DIFERENTE_DO_TOTAL");
		}
	}

	public void gerarParcelas(CompraParcelada compra, int quantidade) {
		for (int indice = 0; indice < quantidade; indice++) {
			final var parcela = new Gasto();

			parcela.setRealizado(true);
			parcela.setCompraParcelada(compra);
			parcela.setNumeroParcela(indice + 1);
			
			compra.getParcelas().add(parcela);
		}

		recalcularValores(compra);
	}

	public void recalcularValores(CompraParcelada compra) {
		final var quantidade = compra.getParcelas().size();
		final var valorParcela = compra.getValorTotal().divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);

		final var valorPrimeiraParcela = compra.getValorTotal()
				.subtract(valorParcela.multiply(BigDecimal.valueOf(quantidade - 1L)));

		for (final var parcela : compra.getParcelas()) {
			parcela.setValor(parcela.getNumeroParcela() == 1 ? valorPrimeiraParcela : valorParcela);
		}
	}

	public List<Fatura> obterFaturas(MetodoPagamento metodo, YearMonth periodoInicial, int quantidadeParcelas) {
		final var faturas = new ArrayList<Fatura>(quantidadeParcelas);

		for (int indice = 0; indice < quantidadeParcelas; indice++) {
			faturas.add(faturaService.obterOuCriar(metodo.getId(), periodoInicial.plusMonths(indice)));
		}

		return faturas;
	}

	public void aplicarDados(
			CompraParcelada compra, LocalDate dataInicial, String nome, String categoria,
			MetodoPagamento metodo, List<Fatura> faturas, String descricao) {
			
		for (final var parcela : compra.getParcelas()) {
			final var indice = parcela.getNumeroParcela() - 1;
			parcela.setData(dataInicial);
			parcela.setNome(nome);
			parcela.setCategoria(categoria);
			parcela.setMetodoPagamento(metodo);
			parcela.setFatura(faturas.get(indice));
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

	public YearMonth resolverPeriodoInicial(
			MetodoPagamento metodo, LocalDate dataCompra, YearMonth periodoSelecionado) {
				
		final var periodo = periodoSelecionado != null
				? periodoSelecionado
				: faturaService.sugerirPeriodo(metodo, dataCompra);
		if (periodo == null) {
			throw new PagamentoInvalidoException("FATURA_OBRIGATORIA_SEM_DATA_DA_COMPRA");
		}
		return periodo;
	}
}
