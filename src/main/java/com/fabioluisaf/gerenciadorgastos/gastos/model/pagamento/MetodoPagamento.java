package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento;

import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Boleto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Credito;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Debito;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Doc;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Pix;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Ted;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "metodos_pagamento")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class MetodoPagamento {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Setter
	private UUID id;

	@Column(nullable = false, insertable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	private TipoMetodoPagamento tipo;

	@Column(name = "instituicao_financeira", nullable = false)
	@Enumerated(EnumType.STRING)
	@Setter
	private InstituicaoFinanceira instituicaoFinanceira;

	@Column(nullable = false)
	@Setter
	private String nome;

	public static MetodoPagamento criar(TipoMetodoPagamento tipo, Integer diaFechamento, Integer diaVencimento) {
		if (tipo == null) {
			throw new IllegalArgumentException("Tipo de método de pagamento é obrigatório");
		}

		final MetodoPagamento metodo = switch (tipo) {
			case CREDITO -> new Credito(diaFechamento, diaVencimento);
			case DEBITO -> new Debito();
			case PIX -> new Pix();
			case BOLETO -> new Boleto();
			case DOC -> new Doc();
			case TED -> new Ted();
		};

		if (tipo != TipoMetodoPagamento.CREDITO) {
			metodo.atualizarDiaFechamento(diaFechamento);
			metodo.atualizarDiaVencimento(diaVencimento);
		}
        
		return metodo;
	}

	protected MetodoPagamento(TipoMetodoPagamento tipo) {
		this.tipo = tipo;
	}

	public Integer getDiaFechamento() {
		return null;
	}

	public void atualizarDiaFechamento(Integer diaFechamento) {
		if (diaFechamento != null) {
			throw new PagamentoInvalidoException("DIA_FECHAMENTO_PERMITIDO_APENAS_PARA_CREDITO");
		}
	}

	public Integer getDiaVencimento() {
		return null;
	}

	public void atualizarDiaVencimento(Integer diaVencimento) {
		if (diaVencimento != null) {
			throw new PagamentoInvalidoException("DIA_VENCIMENTO_PERMITIDO_APENAS_PARA_CREDITO");
		}
	}

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof MetodoPagamento outro)) {
			return false;
		}
		final var identificador = getId();

		return identificador != null && identificador.equals(outro.getId());
	}

	@Override
	public int hashCode() {
		return MetodoPagamento.class.hashCode();
	}
}
