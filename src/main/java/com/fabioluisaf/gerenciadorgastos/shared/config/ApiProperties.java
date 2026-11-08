package com.fabioluisaf.gerenciadorgastos.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api")
public record ApiProperties(String versao) {

	public ApiProperties {
		if (versao == null || versao.isBlank()) {
			versao = "0.0.0.0--nao_informada";
		}
	}
}
