package com.fabioluisaf.gerenciadorgastos.shared.serialization;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoPeriodo;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class PeriodoDeserializer extends ValueDeserializer<YearMonth> {

	private static final List<DateTimeFormatter> FORMATOS = Arrays.stream(FormatoPeriodo.values())
			.map(formato -> DateTimeFormatter.ofPattern(formato.getFormato()).withResolverStyle(ResolverStyle.STRICT))
			.toList();

	@Override
	public YearMonth deserialize(JsonParser parser, DeserializationContext context) {
		if (!parser.hasToken(JsonToken.VALUE_STRING)) {
			return context.reportInputMismatch(YearMonth.class, "PERIODO_FATURA_INVALIDO");
		}

		final var valor = parser.getString();

		for (final var formato : FORMATOS) {
			try {
				return YearMonth.parse(valor, formato);
			} catch (DateTimeParseException _) {
				// Try the next supported format before reporting invalid input.
			}
		}
        
		return context.reportInputMismatch(YearMonth.class, "PERIODO_FATURA_INVALIDO");
	}
}
