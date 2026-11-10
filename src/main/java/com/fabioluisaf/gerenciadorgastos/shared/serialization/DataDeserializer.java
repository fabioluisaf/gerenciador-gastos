package com.fabioluisaf.gerenciadorgastos.shared.serialization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class DataDeserializer extends ValueDeserializer<LocalDate> {

	private static final List<DateTimeFormatter> FORMATOS = Arrays.stream(FormatoData.values())
			.map(formato -> DateTimeFormatter.ofPattern(formato.getFormato()).withResolverStyle(ResolverStyle.STRICT))
			.toList();

	@Override
	public LocalDate deserialize(JsonParser parser, DeserializationContext context) {
		if (!parser.hasToken(JsonToken.VALUE_STRING)) {
			return context.reportInputMismatch(LocalDate.class, "DATA_INVALIDA");
		}

		final var valor = parser.getString();

		for (final var formato : FORMATOS) {
			try {
				return LocalDate.parse(valor, formato);
			} catch (DateTimeParseException _) {
				// Try the next supported format before reporting invalid input.
			}
		}
        
		return context.reportInputMismatch(LocalDate.class, "DATA_INVALIDA");
	}
}
