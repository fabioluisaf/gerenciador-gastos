package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarInstituicaoFinanceiraBody(

    @NotBlank
    @Size(max = 255)
    String nome,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    String cor,

    MultipartFile icone
) {
}
