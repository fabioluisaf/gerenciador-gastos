package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

public record AplicarGrupoPlanejadoBody(

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate data,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    YearMonth faturaPeriodo,

    @NotEmpty
    @Size(max = 100)
    List<@NotNull @Valid ItemGrupoPlanejadoBody> itens
) {
	@AssertTrue(message = "DATA_OU_FATURA_OBRIGATORIA")
	public boolean isDataOuFaturaInformada() {
		return data != null || faturaPeriodo != null;
	}

	public CriarGastoBody gastoPlanejado(ItemGrupoPlanejadoBody item) {
		return new CriarGastoBody(
				data, item.nome().strip(), item.valor(), item.categoria().strip(), metodoPagamentoId, faturaPeriodo,
				item.descricao(), false
		);
	}
}
