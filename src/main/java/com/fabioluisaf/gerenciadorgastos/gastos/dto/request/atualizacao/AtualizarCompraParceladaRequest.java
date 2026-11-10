package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarCompraParceladaRequest(
    @NotBlank 
    String nome,

    @NotBlank 
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate faturaDataFechamento,

    String descricao,

    @NotNull 
    @PositiveOrZero 
    @Digits(integer = 17, fraction = 2) 
    BigDecimal valorTotal,

    @NotNull 
    @Positive 
    Integer quantidadeParcelas,

    @NotNull
    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate data,

    @Schema(description = "Valores opcionais na ordem das parcelas, com quantidade e soma iguais as da compra. "
            + "Se omitidos, preserva os valores atuais ou recalcula quando o total ou a quantidade mudar.")
    List<@NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal> valoresParcelas
) {
}
