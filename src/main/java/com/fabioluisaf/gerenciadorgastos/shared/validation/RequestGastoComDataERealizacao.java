package com.fabioluisaf.gerenciadorgastos.shared.validation;

import java.time.LocalDate;

public interface RequestGastoComDataERealizacao {

	LocalDate data();

	Boolean realizado();
}
