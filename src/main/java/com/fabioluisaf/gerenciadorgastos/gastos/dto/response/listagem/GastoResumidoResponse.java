package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.annotation.JsonSerialize;

public record GastoResumidoResponse(
    UUID id,

    @JsonSerialize(using = DataSerializer.class)
    LocalDate data,

    String nome,

    BigDecimal valor,

    String categoria,

    MetodoPagamentoResumidoResponse metodoPagamento,

    boolean realizado,
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ParcelaResumidaResponse parcela
) {

	public static GastoResumidoResponse from(Gasto entidade) {
		final var compra = entidade.getCompraParcelada();
		final var parcela = compra == null ? null : new ParcelaResumidaResponse(
				entidade.getNumeroParcela(), compra.getQuantidadeParcelas()
		);

		return new GastoResumidoResponse(
				entidade.getId(),
				entidade.getData(),
				entidade.getNome(),
				entidade.getValor(),
				entidade.getCategoria(),
				MetodoPagamentoResumidoResponse.from(entidade.getMetodoPagamento()),
				entidade.isRealizado(),
				parcela
		);
	}
}
