package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.AplicarGrupoPlanejadoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.ItemGrupoPlanejadoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.SalvarGrupoPlanejadoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.GrupoPlanejado;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GrupoPlanejadoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrupoPlanejadoService {

	private final GrupoPlanejadoRepository grupoPlanejadoRepository;
	private final GastoService gastoService;

	public Page<GrupoPlanejado> listar(Pageable pageable) {
		return grupoPlanejadoRepository.findAll(pageable);
	}

	public GrupoPlanejado buscarPorId(UUID id) {
		return grupoPlanejadoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Grupo planejado não encontrado: " + id));
	}

	@Transactional
	public GrupoPlanejado criar(SalvarGrupoPlanejadoBody request) {
		final var grupo = new GrupoPlanejado();
		preencher(grupo, request);

		return grupoPlanejadoRepository.save(grupo);
	}

	@Transactional
	public GrupoPlanejado atualizar(UUID id, SalvarGrupoPlanejadoBody request) {
		final var grupo = buscarPorId(id);
		preencher(grupo, request);

		return grupoPlanejadoRepository.save(grupo);
	}

	@Transactional
	public void excluir(UUID id) {
		grupoPlanejadoRepository.delete(buscarPorId(id));
	}

	@Transactional
	public List<Gasto> aplicar(UUID id, AplicarGrupoPlanejadoBody request) {
		buscarPorId(id);

		return request.itens().stream()
				.map(item -> gastoService.criar(request.gastoPlanejado(item)))
				.toList();
	}

	private static void preencher(GrupoPlanejado grupo, SalvarGrupoPlanejadoBody request) {
		grupo.setNome(request.nome().strip());
		grupo.substituirItens(request.itens().stream().map(ItemGrupoPlanejadoBody::toEntity).toList());
	}
}
