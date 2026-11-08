package com.fabioluisaf.gerenciadorgastos.shared.dto;

public record DadosPage(
    int pagina,
    int tamanho,
    long totalElementos,
    int totalPaginas
) {
}
