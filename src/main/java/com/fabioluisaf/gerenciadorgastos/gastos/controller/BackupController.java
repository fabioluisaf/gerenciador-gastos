package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.time.LocalDate;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fabioluisaf.gerenciadorgastos.gastos.service.BackupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

	private static final MediaType ZIP = MediaType.parseMediaType("application/zip");

	private final BackupService service;

	@GetMapping
	public ResponseEntity<byte[]> baixar() {
		final var arquivo = ContentDisposition.attachment()
				.filename("backup-gastos-" + LocalDate.now() + ".zip")
				.build();

		return ResponseEntity.ok()
				.contentType(ZIP)
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION, arquivo.toString())
				.body(service.gerar());
	}
}
