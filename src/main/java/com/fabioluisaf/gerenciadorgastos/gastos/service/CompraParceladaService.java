package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
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
	private final Parcelamento parcelamento;

	@Transactional
	public CompraParcelada criar(@NotNull @Valid CriarCompraParceladaRequest request) {
		final var compra = new CompraParcelada();
		final var metodo = buscarMetodoCredito(request.metodoPagamentoId());
		final var dataFechamentoInicial = parcelamento.resolverDataFechamentoInicial(
				metodo, request.data(), request.faturaDataFechamento()
		);
		compra.setValorTotal(request.valorTotal());

		parcelamento.gerarParcelas(compra, request.quantidadeParcelas());
		parcelamento.aplicarDados(compra, request.data(), request.nome(), request.categoria(),
				metodo, dataFechamentoInicial, request.descricao());

		return compraParceladaRepository.save(compra);
	}

	@Transactional
	public CompraParcelada atualizar(@NotNull UUID id, @NotNull @Valid AtualizarCompraParceladaRequest request) {
		final var compra = buscarEntidadePorId(id);
		parcelamento.validarValores(request);
		final var metodo = buscarMetodoCredito(request.metodoPagamentoId());
		final var dataFechamentoInicial = parcelamento.resolverDataFechamentoInicial(
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
			parcelamento.gerarParcelas(compra, request.quantidadeParcelas());
		}

		parcelamento.aplicarDados(compra, request.data(), request.nome(), request.categoria(),
				metodo, dataFechamentoInicial, request.descricao());
		parcelamento.aplicarValores(compra, request.valoresParcelas());

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
}
