package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalGastosResponse;

public interface TotaisGastosRepository {

	TotalGastosResponse totalizar(FiltroGastos filtro);
}
