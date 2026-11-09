package com.fabioluisaf.gerenciadorgastos.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
    T dados, 
    String versao,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    DadosPage paginacao
) {
}
