CREATE UNIQUE INDEX uq_faturas_metodo_mes
    ON faturas (
        metodo_pagamento_id,
        (EXTRACT(YEAR FROM data_fechamento)),
        (EXTRACT(MONTH FROM data_fechamento))
    );
