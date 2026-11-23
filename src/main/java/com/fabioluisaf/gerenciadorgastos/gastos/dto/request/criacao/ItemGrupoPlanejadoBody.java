package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.math.BigDecimal;

import com.fabioluisaf.gerenciadorgastos.gastos.model.ItemGrupoPlanejado;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemGrupoPlanejadoBody(

    @NotBlank
    @Size(max = 255)
    String nome,

    @NotNull
    @Digits(integer = 17, fraction = 2)
    BigDecimal valor,

    @NotBlank
    @Size(max = 255)
    String categoria,

    @Size(max = 255)
    String descricao
) {
    public ItemGrupoPlanejado toEntity() {
        return new ItemGrupoPlanejado(nome.strip(), valor, categoria.strip(), descricao);
    }
}
