package com.fabioluisaf.gerenciadorgastos.shared.dto;

import java.util.List;

public record ApiPageResponse<T>(
    List<T> dados,
    String versao,
    DadosPage paginacao
) {
}
