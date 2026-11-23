package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalvarGrupoPlanejadoBody(

    @NotBlank
    @Size(max = 255)
    String nome,

    @NotEmpty
    @Size(max = 100)
    List<@NotNull @Valid ItemGrupoPlanejadoBody> itens
) {
}
