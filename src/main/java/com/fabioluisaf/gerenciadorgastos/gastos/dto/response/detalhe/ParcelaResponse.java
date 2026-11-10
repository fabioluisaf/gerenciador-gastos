package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InformacoesFaturaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

public record ParcelaResponse(
    UUID id,
    Integer numeroParcela,
    @JsonSerialize(using = DataSerializer.class) LocalDate data,
    BigDecimal valor,
    InformacoesFaturaResponse fatura,
    boolean realizado
) {
    public static ParcelaResponse from(Gasto parcela) {
        return new ParcelaResponse(
            parcela.getId(),
            parcela.getNumeroParcela(),
            parcela.getData(),
            parcela.getValor(),
            InformacoesFaturaResponse.from(parcela.getFatura()),
            parcela.isRealizado()
        );
    }
}
