package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.compraparcelada.InformacoesParcelaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

public record GastoResponse(
    UUID id,

    @JsonFormat(pattern = FormatoData.DIA_MES_ANO) 
    LocalDate data,

    String nome,

    BigDecimal valor,

    String categoria,

    MetodoPagamento metodoPagamento,

    String descricao,

    boolean realizado,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    InformacoesParcelaResponse parcela
) {

	public static GastoResponse from(Gasto entidade) {
		final var compra = entidade.getCompraParcelada();
		final var parcela = compra == null ? null : new InformacoesParcelaResponse(
            entidade.getNumeroParcela(),
            compra.getQuantidadeParcelas(),
            compra.getId()
        );
		return new GastoResponse(
            entidade.getId(),
            entidade.getData(),
            entidade.getNome(),
            entidade.getValor(),
            entidade.getCategoria(),
            entidade.getMetodoPagamento(),
            entidade.getDescricao(),
            entidade.isRealizado(),
            parcela
        );
	}
}
