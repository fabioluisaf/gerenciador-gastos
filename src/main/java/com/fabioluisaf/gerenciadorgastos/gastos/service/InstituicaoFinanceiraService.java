package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarInstituicaoFinanceiraForm;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarInstituicaoFinanceiraForm;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.IconeInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.InstituicaoFinanceiraEmUsoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.InstituicaoFinanceiraExistenteException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.InstituicaoFinanceiraRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstituicaoFinanceiraService {

	private final InstituicaoFinanceiraRepository instituicaoFinanceiraRepository;
	private final MetodoPagamentoRepository metodoPagamentoRepository;

	public Page<InstituicaoFinanceira> listar(Pageable pageable) {
		return instituicaoFinanceiraRepository.findAll(pageable);
	}

	public InstituicaoFinanceira buscarPorId(UUID id) {
		return instituicaoFinanceiraRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Instituição financeira não encontrada: " + id));
	}

	@Transactional
	public InstituicaoFinanceira criar(CriarInstituicaoFinanceiraForm form) {
		final var nome = form.nome().strip();
		if (instituicaoFinanceiraRepository.existsByNomeIgnoreCase(nome)) {
			throw new InstituicaoFinanceiraExistenteException();
		}

		final var instituicao = new InstituicaoFinanceira(nome);
		instituicao.setCor(normalizarCor(form.cor()));
		aplicarIcone(instituicao, form.icone());

		return instituicaoFinanceiraRepository.save(instituicao);
	}

	@Transactional
	public InstituicaoFinanceira atualizar(UUID id, AtualizarInstituicaoFinanceiraForm form) {
		final var instituicao = buscarPorId(id);
		final var nome = form.nome().strip();
		if (instituicaoFinanceiraRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
			throw new InstituicaoFinanceiraExistenteException();
		}

		instituicao.setNome(nome);
		instituicao.setCor(normalizarCor(form.cor()));
		aplicarIcone(instituicao, form.icone());

		return instituicaoFinanceiraRepository.save(instituicao);
	}

	@Transactional
	public void excluir(UUID id) {
		final var instituicao = buscarPorId(id);
		if (metodoPagamentoRepository.existsByInstituicaoFinanceiraId(id)) {
			throw new InstituicaoFinanceiraEmUsoException();
		}

		instituicaoFinanceiraRepository.delete(instituicao);
	}

	private static String normalizarCor(String cor) {
		return cor == null || cor.isBlank() ? null : cor.toUpperCase(Locale.ROOT);
	}

	private void aplicarIcone(InstituicaoFinanceira instituicao, MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			return;
		}
		if (arquivo.getSize() > InstituicaoFinanceira.TAMANHO_MAXIMO_ICONE) {
			throw new IconeInvalidoException("ICONE_MUITO_GRANDE");
		}

		final byte[] conteudo;
		try {
			conteudo = arquivo.getBytes();
		} catch (IOException excecao) {
			throw new UncheckedIOException(excecao);
		}

		instituicao.atualizarIcone(conteudo, detectarTipoImagem(conteudo));
	}

	static String detectarTipoImagem(byte[] conteudo) {
		if (comecaCom(conteudo, 0, new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' })) {
			return "image/png";
		}
		if (comecaCom(conteudo, 0, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })) {
			return "image/jpeg";
		}
		if (comecaCom(conteudo, 0, ascii("GIF87a")) || comecaCom(conteudo, 0, ascii("GIF89a"))) {
			return "image/gif";
		}
		if (comecaCom(conteudo, 0, ascii("RIFF")) && comecaCom(conteudo, 8, ascii("WEBP"))) {
			return "image/webp";
		}

		throw new IconeInvalidoException("FORMATO_ICONE_INVALIDO");
	}

	private static byte[] ascii(String texto) {
		return texto.getBytes(StandardCharsets.US_ASCII);
	}

	private static boolean comecaCom(byte[] conteudo, int posicao, byte[] assinatura) {
		return conteudo.length >= posicao + assinatura.length
				&& Arrays.equals(conteudo, posicao, posicao + assinatura.length, assinatura, 0, assinatura.length);
	}
}
