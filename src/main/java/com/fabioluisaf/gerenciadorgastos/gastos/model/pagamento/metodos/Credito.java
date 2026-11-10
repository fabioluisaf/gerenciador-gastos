package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos;

import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CREDITO")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credito extends MetodoPagamento {

	@Getter
	@Column(name = "dia_fechamento")
	private Integer diaFechamento;

	public Credito(Integer diaFechamento) {
		super(TipoMetodoPagamento.CREDITO);
		atualizarDiaFechamento(diaFechamento);
	}

	@Override
	public void atualizarDiaFechamento(Integer diaFechamento) {
		if (diaFechamento == null || diaFechamento < 1 || diaFechamento > 31) {
			throw new PagamentoInvalidoException("DIA_FECHAMENTO_OBRIGATORIO_PARA_CREDITO");
		}
		this.diaFechamento = diaFechamento;
	}

    @Override 
    public boolean equals(Object objeto) {
        return super.equals(objeto);
    }

    @Override 
    public int hashCode() {
        return super.hashCode();
    }
}
