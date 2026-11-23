package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe;

import java.math.BigDecimal;

import com.fabioluisaf.gerenciadorgastos.gastos.model.ItemGrupoPlanejado;

public record ItemGrupoPlanejadoResponse(
    String nome,
    BigDecimal valor,
    String categoria,
    String descricao
) {
    public static ItemGrupoPlanejadoResponse from(ItemGrupoPlanejado item) {
        return new ItemGrupoPlanejadoResponse(item.getNome(), item.getValor(), item.getCategoria(), item.getDescricao());
    }
}
