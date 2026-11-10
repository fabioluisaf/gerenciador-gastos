package com.fabioluisaf.gerenciadorgastos.shared.constantes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormatoData {
	DIA_MES_ANO("dd/MM/uuuu"),
	DIA_MES_ANO_HIFEN("dd-MM-uuuu"),
	ANO_MES_DIA_HIFEN("uuuu-MM-dd");

	private final String formato;
}
