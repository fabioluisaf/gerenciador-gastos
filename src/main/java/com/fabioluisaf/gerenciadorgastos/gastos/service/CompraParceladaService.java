package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
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

	@Transactional
	public CompraParcelada criar(@NotNull @Valid CriarCompraParceladaRequest request) {
		final var compra = new CompraParcelada();
		compra.setValorTotal(request.valorTotal());

		gerarParcelas(compra, request.data(), request.quantidadeParcelas(), request.nome(),
				request.categoria(), request.metodoPagamento(), request.descricao());

		return compraParceladaRepository.save(compra);
	}

	@Transactional
	public CompraParcelada atualizar(@NotNull UUID id, @NotNull @Valid AtualizarCompraParceladaRequest request) {
		final var compra = buscarEntidadePorId(id);
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
			gerarParcelas(compra, request.data(), request.quantidadeParcelas(), request.nome(),
					request.categoria(), request.metodoPagamento(), request.descricao());
		} else {
			for (final var parcela : parcelas) {
				parcela.setNome(request.nome());
				parcela.setCategoria(request.categoria());
				parcela.setMetodoPagamento(request.metodoPagamento());
				parcela.setDescricao(request.descricao());
				parcela.setData(dataParcela(request.data(), parcela.getNumeroParcela() - 1));
			}
		}
        
		return compraParceladaRepository.saveAndFlush(compra);
	}

	private void gerarParcelas(
			CompraParcelada compra, LocalDate dataInicial, int quantidade, String nome,
			String categoria, MetodoPagamento metodoPagamento, String descricao
	) {
		final var valorParcela = compra.getValorTotal().divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);
		final var valorPrimeiraParcela = compra.getValorTotal()
				.subtract(valorParcela.multiply(BigDecimal.valueOf(quantidade - 1L)));
		for (int indice = 0; indice < quantidade; indice++) {
			final var parcela = new Gasto();
			parcela.setData(dataParcela(dataInicial, indice));
			parcela.setNome(nome);
			parcela.setValor(indice == 0 ? valorPrimeiraParcela : valorParcela);
			parcela.setCategoria(categoria);
			parcela.setMetodoPagamento(metodoPagamento);
			parcela.setDescricao(descricao);
			parcela.setRealizado(true);
			parcela.setCompraParcelada(compra);
			parcela.setNumeroParcela(indice + 1);
			compra.getParcelas().add(parcela);
		}
	}

	private LocalDate dataParcela(LocalDate dataInicial, int indice) {
		return indice == 0 ? dataInicial : dataInicial.withDayOfMonth(1).plusMonths(indice);
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
}
