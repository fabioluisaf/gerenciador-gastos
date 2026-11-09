package com.fabioluisaf.gerenciadorgastos.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

class ConfiguracaoPerfisTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer());

	@Test
	void documentacaoDisponivelPorPadrao() {
		contextRunner.run(contexto -> {
			final Environment ambiente = contexto.getEnvironment();
			assertEquals("true", ambiente.getProperty("spring.jpa.open-in-view"));
			assertEquals("true", ambiente.getProperty("springdoc.api-docs.enabled"));
			assertEquals("true", ambiente.getProperty("springdoc.swagger-ui.enabled"));
			assertEquals("/swagger", ambiente.getProperty("springdoc.swagger-ui.path"));
		});
	}

	@Test
	void documentacaoDesativadaEmProducao() {
		new ApplicationContextRunner()
				.withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles("prod"))
				.withInitializer(new ConfigDataApplicationContextInitializer())
				.run(contexto -> {
					final Environment ambiente = contexto.getEnvironment();
					assertEquals("true", ambiente.getProperty("spring.jpa.open-in-view"));
					assertEquals("false", ambiente.getProperty("springdoc.api-docs.enabled"));
					assertEquals("false", ambiente.getProperty("springdoc.swagger-ui.enabled"));
				});
	}
}
