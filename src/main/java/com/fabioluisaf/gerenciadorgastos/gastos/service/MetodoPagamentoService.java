package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.MetodoPagamentoEmUsoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Credito;
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

	@Transactional
	public MetodoPagamento criar(CriarMetodoPagamentoRequest request) {
		final var metodo = MetodoPagamento.criar(request.tipo(), request.diaFechamento(), request.diaVencimento());
		aplicarDados(metodo, request.instituicaoFinanceira(), request.nome());

		return metodoPagamentoRepository.save(metodo);
	}

	@Transactional
	public MetodoPagamento atualizar(UUID id, AtualizarMetodoPagamentoRequest request) {
		final var metodo = buscarEntidadePorId(id);

		metodo.atualizarDiaFechamento(request.diaFechamento());
		metodo.atualizarDiaVencimento(request.diaVencimento());
		aplicarDados(metodo, request.instituicaoFinanceira(), request.nome());

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
			MetodoPagamento metodo, InstituicaoFinanceira instituicao, String nome
	) {
		metodo.setInstituicaoFinanceira(instituicao);
		metodo.setNome(nome);
	}

	public MetodoPagamento buscarEntidadePorId(UUID id) {
		return metodoPagamentoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Método de pagamento não encontrado: " + id));
	}

	public Credito buscarCreditoPorId(UUID id, String codigoErro) {
		final var metodo = buscarEntidadePorId(id);
        
		if (!(Hibernate.unproxy(metodo) instanceof Credito credito)) {
			throw new PagamentoInvalidoException(codigoErro);
		}

		return credito;
	}
}
