package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.validation.DataObrigatoriaSeRealizado;
import com.fabioluisaf.gerenciadorgastos.shared.validation.RequestGastoComDataERealizacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.annotation.JsonDeserialize;

@DataObrigatoriaSeRealizado
public record CriarGastoBody(
    
    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate data,

    @NotBlank
    String nome,

    @NotNull
    @PositiveOrZero
    BigDecimal valor,

    @NotBlank
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    YearMonth faturaPeriodo,

    String descricao,

    @NotNull
    Boolean realizado
) implements RequestGastoComDataERealizacao {
    public Gasto toEntity() {
        final var gasto = new Gasto();
        gasto.setData(data);
        gasto.setNome(nome);
        gasto.setValor(valor);
        gasto.setCategoria(categoria);
        gasto.setDescricao(descricao);
        gasto.setRealizado(realizado);
        
        return gasto;
    }
}
