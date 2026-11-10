package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.compraparcelada;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;

public record CompraParceladaResponse(
    UUID id,
    BigDecimal valorTotal,
    int quantidadeParcelas,
    List<ParcelaResponse> parcelas
) {
    public static CompraParceladaResponse from(CompraParcelada compra) {
        final var parcelas = compra.getParcelas().stream()
                .sorted(Comparator.comparingInt(parcela ->
                        Objects.requireNonNull(Objects.requireNonNull(parcela).getNumeroParcela())))
                .map(ParcelaResponse::from)
                .toList();

        return new CompraParceladaResponse(compra.getId(), compra.getValorTotal(), parcelas.size(), parcelas);
    }
}
