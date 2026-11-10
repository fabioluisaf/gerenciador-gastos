package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DOC")
public class Doc extends MetodoPagamento {

	public Doc() {
		super(TipoMetodoPagamento.DOC);
	}
}
