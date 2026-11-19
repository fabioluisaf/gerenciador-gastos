package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instituicoes_financeiras")
@Getter
@Setter
@NoArgsConstructor
public class InstituicaoFinanceira {

	public static final int TAMANHO_MAXIMO_ICONE = 1024 * 1024;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String nome;

	@Column(length = TAMANHO_MAXIMO_ICONE)
	private byte[] icone;

	@Column(name = "icone_tipo", length = 100)
	private String iconeTipo;

	@Column(length = 7)
	private String cor;

	public InstituicaoFinanceira(String nome) {
		this.nome = nome;
	}

	public boolean possuiIcone() {
		return icone != null;
	}

	public void atualizarIcone(byte[] conteudo, String tipo) {
		this.icone = conteudo;
		this.iconeTipo = conteudo == null ? null : tipo;
	}

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof InstituicaoFinanceira outra)) {
			return false;
		}
		final var identificador = getId();

		return identificador != null && identificador.equals(outra.getId());
	}

	@Override
	public int hashCode() {
		return InstituicaoFinanceira.class.hashCode();
	}
}
