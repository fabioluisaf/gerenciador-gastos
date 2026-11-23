package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.GrupoPlanejado;

public record GrupoPlanejadoResponse(
    UUID id,
    String nome,
    BigDecimal valorTotal,
    List<ItemGrupoPlanejadoResponse> itens
) {
    public static GrupoPlanejadoResponse from(GrupoPlanejado grupo) {
        return new GrupoPlanejadoResponse(
                grupo.getId(),
                grupo.getNome(),
                grupo.getValorTotal(),
                grupo.getItens().stream().map(ItemGrupoPlanejadoResponse::from).toList()
        );
    }
}
