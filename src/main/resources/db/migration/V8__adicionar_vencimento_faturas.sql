ALTER TABLE metodos_pagamento ADD COLUMN dia_vencimento INTEGER;
ALTER TABLE faturas ADD COLUMN data_vencimento DATE;

UPDATE faturas
SET data_vencimento = data_fechamento + 7;

UPDATE metodos_pagamento m
SET dia_vencimento = COALESCE(
    (SELECT EXTRACT(DAY FROM f.data_vencimento)::INTEGER
     FROM faturas f
     WHERE f.metodo_pagamento_id = m.id
     ORDER BY f.data_fechamento DESC
     LIMIT 1),
    (m.dia_fechamento + 6) % 31 + 1
)
WHERE m.tipo = 'CREDITO';

ALTER TABLE metodos_pagamento
    ADD CONSTRAINT ck_metodos_pagamento_vencimento CHECK (
        (tipo = 'CREDITO' AND dia_vencimento IS NOT NULL AND dia_vencimento BETWEEN 1 AND 31)
        OR (tipo <> 'CREDITO' AND dia_vencimento IS NULL)
    );

ALTER TABLE faturas ALTER COLUMN data_vencimento SET NOT NULL;
