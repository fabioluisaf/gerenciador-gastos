package com.fabioluisaf.gerenciadorgastos.shared.constantes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormatoPeriodo {
	ANO_MES_HIFEN("uuuu-MM"),
	MES_ANO_HIFEN("MM-uuuu"),
	ANO_MES_PONTO("uuuu.MM"),
	MES_ANO_PONTO("MM.uuuu"),
	MES_ANO("MM/uuuu");

	private final String formato;
}
