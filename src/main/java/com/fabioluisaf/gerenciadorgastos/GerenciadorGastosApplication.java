package com.fabioluisaf.gerenciadorgastos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApiProperties.class)
public class GerenciadorGastosApplication {

	public static void main(String[] args) {
		SpringApplication.run(GerenciadorGastosApplication.class, args);
	}

}
