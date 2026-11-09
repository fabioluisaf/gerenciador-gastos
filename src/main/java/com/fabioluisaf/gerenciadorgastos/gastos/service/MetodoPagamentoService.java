package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.metodopagamento.AtualizarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.metodopagamento.CriarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.MetodoPagamentoEmUsoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetodoPagamentoService {

	private final MetodoPagamentoRepository metodoPagamentoRepository;
	private final GastoRepository gastoRepository;
	private final FaturaRepository faturaRepository;

	public Page<MetodoPagamento> listar(Pageable pageable) {
		return metodoPagamentoRepository.findAll(pageable);
	}

	public MetodoPagamento buscarPorId(UUID id) {
		return buscarEntidadePorId(id);
	}

	@Transactional
	public MetodoPagamento criar(CriarMetodoPagamentoRequest request) {
		validarConfiguracao(request.tipo(), request.diaFechamento());
		final var metodo = new MetodoPagamento();
		aplicarDados(metodo, request.tipo(), request.instituicaoFinanceira(), request.nome(),
				request.diaFechamento());

		return metodoPagamentoRepository.save(metodo);
	}

	@Transactional
	public MetodoPagamento atualizar(UUID id, AtualizarMetodoPagamentoRequest request) {
		validarConfiguracao(request.tipo(), request.diaFechamento());
		final var metodo = buscarEntidadePorId(id);
		aplicarDados(metodo, request.tipo(), request.instituicaoFinanceira(), request.nome(),
				request.diaFechamento());

		return metodoPagamentoRepository.save(metodo);
	}

	@Transactional
	public void excluir(UUID id) {
		final var metodo = buscarEntidadePorId(id);
		if (gastoRepository.existsByMetodoPagamentoId(id) || faturaRepository.existsByMetodoPagamentoId(id)) {
			throw new MetodoPagamentoEmUsoException();
		}

		metodoPagamentoRepository.delete(metodo);
	}

	private void aplicarDados(
			MetodoPagamento metodo, TipoMetodoPagamento tipo, InstituicaoFinanceira instituicao,
			String nome, Integer diaFechamento
	) {
		metodo.setTipo(tipo);
		metodo.setInstituicaoFinanceira(instituicao);
		metodo.setNome(nome);
		metodo.setDiaFechamento(diaFechamento);
	}

	private void validarConfiguracao(TipoMetodoPagamento tipo, Integer diaFechamento) {
		if (tipo == TipoMetodoPagamento.CREDITO && (diaFechamento == null || diaFechamento < 1
				|| diaFechamento > 31)) {
			throw new PagamentoInvalidoException("DIA_FECHAMENTO_OBRIGATORIO_PARA_CREDITO");
		}
		if (tipo != TipoMetodoPagamento.CREDITO && diaFechamento != null) {
			throw new PagamentoInvalidoException("DIA_FECHAMENTO_PERMITIDO_APENAS_PARA_CREDITO");
		}
	}

	MetodoPagamento buscarEntidadePorId(UUID id) {
		return metodoPagamentoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Método de pagamento não encontrado: " + id));
	}
}
