package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fabioluisaf.gerenciadorgastos.gastos.service.BackupService;

class BackupControllerTest {

	@Test
	void baixaOZipComoAnexo() throws Exception {
		final var service = mock(BackupService.class);
		final var zip = new byte[] { 'P', 'K', 3, 4 };
		when(service.gerar()).thenReturn(zip);
		final var mvc = MockMvcBuilders.standaloneSetup(new BackupController(service)).build();

		mvc.perform(get("/backup"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/zip"))
				.andExpect(content().bytes(zip))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Content-Disposition",
						matchesPattern("attachment; filename=\"backup-gastos-\\d{4}-\\d{2}-\\d{2}\\.zip\"")));
	}
}
