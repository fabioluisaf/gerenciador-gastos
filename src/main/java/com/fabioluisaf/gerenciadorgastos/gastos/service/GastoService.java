package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.time.YearMonth;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.BuscarGastosPorMesEAnoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GastoService {

	private final GastoRepository gastoRepository;

	@Transactional
	public Gasto criar(CriarGastoRequest request) {
		final var entidade = request.toEntity();

		return gastoRepository.save(entidade);
	}

	public Page<Gasto> listarPorMesEAno(BuscarGastosPorMesEAnoRequest request, Pageable pageable) {
		final var periodo = YearMonth.of(request.ano(), request.mes());
		return gastoRepository.findByDataBetween(periodo.atDay(1), periodo.atEndOfMonth(), pageable);
	}

	public Gasto buscarPorId(UUID id) {
		return buscarEntidadePorId(id);
	}

	@Transactional
	public Gasto atualizar(UUID id, AtualizarGastoRequest request) {
		final var entidade = buscarEntidadePorId(id);
		validarGastoAvulso(entidade);
		entidade.setData(request.data());
		entidade.setNome(request.nome());
		entidade.setValor(request.valor());
		entidade.setCategoria(request.categoria());
		entidade.setMetodoPagamento(request.metodoPagamento());
		entidade.setDescricao(request.descricao());
		entidade.setRealizado(request.realizado());
        
		return gastoRepository.save(entidade);
	}

	@Transactional
	public Gasto atualizarRealizado(UUID id, boolean realizado) {
		final var entidade = buscarEntidadePorId(id);
		entidade.setRealizado(realizado);
		return gastoRepository.save(entidade);
	}

	@Transactional
	public void excluir(UUID id) {
		final var gasto = buscarEntidadePorId(id);
		validarGastoAvulso(gasto);
		gastoRepository.delete(gasto);
	}

	private void validarGastoAvulso(Gasto gasto) {
		if (gasto.getCompraParcelada() != null) {
			throw new ParcelaOperacaoInvalidaException();
		}
	}

	private Gasto buscarEntidadePorId(UUID id) {
		return gastoRepository
                .findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Gasto não encontrado: " + id));
	}

}
