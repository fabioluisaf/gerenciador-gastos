package com.fabioluisaf.gerenciadorgastos.shared.serialization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class DataSerializer extends ValueSerializer<LocalDate> {

	private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern(FormatoData.DIA_MES_ANO.getFormato());

	@Override
	public void serialize(LocalDate valor, JsonGenerator generator, SerializationContext context) {
		generator.writeString(valor.format(FORMATO));
	}
}
