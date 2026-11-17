package com.fabioluisaf.gerenciadorgastos.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

class ConfiguracaoPerfisTest {

	private static ApplicationContextRunner contextRunner(final String... perfis) {
		return new ApplicationContextRunner()
				.withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles(perfis))
				.withInitializer(new ConfigDataApplicationContextInitializer());
	}

	@Test
	void documentacaoDesativadaPorPadrao() {
		contextRunner().run(contexto -> {
			final Environment ambiente = contexto.getEnvironment();
			assertEquals("true", ambiente.getProperty("spring.jpa.open-in-view"));
			assertEquals("false", ambiente.getProperty("springdoc.api-docs.enabled"));
			assertEquals("false", ambiente.getProperty("springdoc.swagger-ui.enabled"));
			assertEquals("/swagger", ambiente.getProperty("springdoc.swagger-ui.path"));
			assertNull(ambiente.getProperty("logging.level.org.hibernate.SQL"));
		});
	}

	@Test
	void documentacaoESqlDisponiveisEmDesenvolvimento() {
		contextRunner("dev").run(contexto -> {
			final Environment ambiente = contexto.getEnvironment();
			assertEquals("true", ambiente.getProperty("spring.jpa.open-in-view"));
			assertEquals("true", ambiente.getProperty("springdoc.api-docs.enabled"));
			assertEquals("true", ambiente.getProperty("springdoc.swagger-ui.enabled"));
			assertEquals("/swagger", ambiente.getProperty("springdoc.swagger-ui.path"));
			assertEquals("DEBUG", ambiente.getProperty("logging.level.org.hibernate.SQL"));
		});
	}

	@Test
	void documentacaoESqlDesativadosEmProducao() {
		contextRunner("prod").run(contexto -> {
			final Environment ambiente = contexto.getEnvironment();
			assertEquals("true", ambiente.getProperty("spring.jpa.open-in-view"));
			assertEquals("false", ambiente.getProperty("springdoc.api-docs.enabled"));
			assertEquals("false", ambiente.getProperty("springdoc.swagger-ui.enabled"));
			assertEquals("WARN", ambiente.getProperty("logging.level.org.hibernate.SQL"));
		});
	}
}
