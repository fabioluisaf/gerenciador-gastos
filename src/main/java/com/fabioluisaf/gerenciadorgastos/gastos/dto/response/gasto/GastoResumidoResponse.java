package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.compraparcelada.ParcelaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.metodopagamento.MetodoPagamentoResumidoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

public record GastoResumidoResponse(
    UUID id,

    @JsonFormat(pattern = FormatoData.DIA_MES_ANO)
    LocalDate data,

    String nome,

    BigDecimal valor,

    String categoria,

    MetodoPagamentoResumidoResponse metodoPagamento,

    boolean realizado,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Numero da parcela atual e quantidade total; omitido em gastos avulsos.")
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
