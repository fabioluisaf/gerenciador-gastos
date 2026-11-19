package com.fabioluisaf.gerenciadorgastos.gastos.dto.request;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SalvarInstituicaoFinanceiraForm(

    @NotBlank
    @Size(max = 255)
    String nome,

    MultipartFile icone
) {
}
