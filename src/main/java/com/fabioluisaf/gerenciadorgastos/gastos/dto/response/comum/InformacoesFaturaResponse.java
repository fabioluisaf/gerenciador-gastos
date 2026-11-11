package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum;

import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;

public record InformacoesFaturaResponse(
    UUID id,
    LocalDate dataFechamento
) {

	public static InformacoesFaturaResponse from(Fatura fatura) {
		return new InformacoesFaturaResponse(fatura.getId(), fatura.getDataFechamento());
	}
}
