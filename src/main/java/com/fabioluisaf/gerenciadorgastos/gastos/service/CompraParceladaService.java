package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
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
	private final ParcelamentoComponent parcelamentoComponent;
	private final ObjectProvider<CompraParceladaService> self;

	@Transactional
	public CompraParcelada criar(@NotNull @Valid CriarCompraParceladaRequest request) {
		final var compra = request.toEntity();
		final var metodo = metodoPagamentoService.buscarCreditoPorId(
				request.metodoPagamentoId(), "COMPRA_PARCELADA_EXIGE_CREDITO"
		);
		final var periodoInicial = parcelamentoComponent.resolverPeriodoInicial(
				metodo, request.data(), request.faturaPeriodo()
		);

		parcelamentoComponent.gerarParcelas(compra, request.quantidadeParcelas());
		parcelamentoComponent.aplicarDados(
            compra, request.data(), request.nome(), request.categoria(),
            metodo, periodoInicial, request.descricao()
        );

		return compraParceladaRepository.save(compra);
	}

	@Transactional
	public CompraParcelada atualizar(@NotNull UUID id, @NotNull @Valid AtualizarCompraParceladaRequest request) {
		parcelamentoComponent.validarValores(request);
		final var metodo = metodoPagamentoService.buscarCreditoPorId(
				request.metodoPagamentoId(), "COMPRA_PARCELADA_EXIGE_CREDITO"
		);
		final var periodoInicial = parcelamentoComponent.resolverPeriodoInicial(
				metodo, request.data(), request.faturaPeriodo()
		);

		final var compra = self.getObject().buscarPorId(id);
		final var parcelas = compra.getParcelas();
		final var recriar = compra.getValorTotal().compareTo(request.valorTotal()) != 0
				|| parcelas.size() != request.quantidadeParcelas();

		if (recriar) {
			parcelas.clear();
			compraParceladaRepository.flush();

			compra.setValorTotal(request.valorTotal());
			parcelamentoComponent.gerarParcelas(compra, request.quantidadeParcelas());
		}

		parcelamentoComponent.aplicarDados(
            compra, request.data(), request.nome(), request.categoria(),
            metodo, periodoInicial, request.descricao()
        );
		parcelamentoComponent.aplicarValores(compra, request.valoresParcelas());

		return compraParceladaRepository.saveAndFlush(compra);
	}

	@Transactional
	public void excluir(UUID id) {
		compraParceladaRepository.delete(self.getObject().buscarPorId(id));
	}

	@Transactional(readOnly = true)
	public CompraParcelada buscarPorId(UUID id) {
		return compraParceladaRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Compra parcelada não encontrada: " + id));
	}
}
