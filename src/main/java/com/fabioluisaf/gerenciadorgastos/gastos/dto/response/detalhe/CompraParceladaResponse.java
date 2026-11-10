package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.MetodoPagamentoResponse;

public record CompraParceladaResponse(
    UUID id,
    String nome,
    String categoria,
    MetodoPagamentoResponse metodoPagamento,
    String descricao,
    BigDecimal valorTotal,
    int quantidadeParcelas,
    List<ParcelaResponse> parcelas
) {
    public static CompraParceladaResponse from(CompraParcelada compra) {
        final var parcelas = compra.getParcelas().stream()
                .sorted(Comparator.comparingInt(parcela ->
                        Objects.requireNonNull(Objects.requireNonNull(parcela).getNumeroParcela())))
                .toList();
        final var primeiraParcela = parcelas.getFirst();

        return new CompraParceladaResponse(
                compra.getId(),
                primeiraParcela.getNome(),
                primeiraParcela.getCategoria(),
                MetodoPagamentoResponse.from(primeiraParcela.getMetodoPagamento()),
                primeiraParcela.getDescricao(),
                compra.getValorTotal(),
                parcelas.size(),
                parcelas.stream().map(ParcelaResponse::from).toList()
        );
    }
}
