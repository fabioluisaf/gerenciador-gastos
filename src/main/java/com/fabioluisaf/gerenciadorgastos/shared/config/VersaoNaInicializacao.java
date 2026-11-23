package com.fabioluisaf.gerenciadorgastos.shared.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VersaoNaInicializacao {

	private final ApiProperties apiProperties;

	@EventListener(ApplicationReadyEvent.class)
	public void registrarVersao() {
		log.info("Gerenciador de gastos API versão {}", apiProperties.versao());
	}
}
