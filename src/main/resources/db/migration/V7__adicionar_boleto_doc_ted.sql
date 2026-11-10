ALTER TABLE metodos_pagamento
    DROP CONSTRAINT ck_metodos_pagamento_tipo,
    DROP CONSTRAINT ck_metodos_pagamento_fechamento,
    ADD CONSTRAINT ck_metodos_pagamento_tipo CHECK (
        tipo IN ('CREDITO', 'DEBITO', 'PIX', 'BOLETO', 'DOC', 'TED')
    ),
    ADD CONSTRAINT ck_metodos_pagamento_fechamento CHECK (
        (tipo = 'CREDITO' AND dia_fechamento IS NOT NULL AND dia_fechamento BETWEEN 1 AND 31)
        OR (tipo IN ('DEBITO', 'PIX', 'BOLETO', 'DOC', 'TED') AND dia_fechamento IS NULL)
    );

ALTER TABLE gastos
    DROP CONSTRAINT ck_gastos_tipo_metodo,
    DROP CONSTRAINT ck_gastos_fatura_por_tipo,
    ADD CONSTRAINT ck_gastos_tipo_metodo CHECK (
        tipo_metodo_pagamento IN ('CREDITO', 'DEBITO', 'PIX', 'BOLETO', 'DOC', 'TED')
    ),
    ADD CONSTRAINT ck_gastos_fatura_por_tipo CHECK (
        (tipo_metodo_pagamento = 'CREDITO' AND fatura_id IS NOT NULL)
        OR (tipo_metodo_pagamento IN ('DEBITO', 'PIX', 'BOLETO', 'DOC', 'TED') AND fatura_id IS NULL)
    );
