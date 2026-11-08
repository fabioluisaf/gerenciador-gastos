package com.fabioluisaf.gerenciadorgastos.shared.dto;

public record ApiResponse<T>(
    T dados, 
    String versao
) {
}
