CREATE TABLE metodos_pagamento (
    id UUID PRIMARY KEY,
    tipo VARCHAR(16) NOT NULL,
    instituicao_financeira VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    dia_fechamento INTEGER,
    CONSTRAINT uq_metodos_pagamento_id_tipo UNIQUE (id, tipo),
    CONSTRAINT ck_metodos_pagamento_fechamento CHECK (
        (tipo = 'CREDITO' AND dia_fechamento BETWEEN 1 AND 31)
        OR (tipo IN ('DEBITO', 'PIX') AND dia_fechamento IS NULL)
    ),
    CONSTRAINT ck_metodos_pagamento_tipo CHECK (tipo IN ('CREDITO', 'DEBITO', 'PIX')),
    CONSTRAINT ck_metodos_pagamento_instituicao CHECK (instituicao_financeira IN ('NUBANK', 'CAIXA'))
);

CREATE TABLE faturas (
    id UUID PRIMARY KEY,
    metodo_pagamento_id UUID NOT NULL,
    tipo_metodo_pagamento VARCHAR(16) NOT NULL DEFAULT 'CREDITO',
    data_fechamento DATE NOT NULL,
    CONSTRAINT uq_faturas_metodo_data UNIQUE (metodo_pagamento_id, data_fechamento),
    CONSTRAINT uq_faturas_id_metodo UNIQUE (id, metodo_pagamento_id),
    CONSTRAINT ck_faturas_somente_credito CHECK (tipo_metodo_pagamento = 'CREDITO'),
    CONSTRAINT fk_faturas_metodo_credito
        FOREIGN KEY (metodo_pagamento_id, tipo_metodo_pagamento)
        REFERENCES metodos_pagamento (id, tipo)
);

ALTER TABLE gastos
    DROP COLUMN metodo_pagamento,
    ADD COLUMN metodo_pagamento_id UUID NOT NULL,
    ADD COLUMN tipo_metodo_pagamento VARCHAR(16) NOT NULL,
    ADD COLUMN fatura_id UUID,
    ADD CONSTRAINT fk_gastos_metodo_tipo
        FOREIGN KEY (metodo_pagamento_id, tipo_metodo_pagamento)
        REFERENCES metodos_pagamento (id, tipo),
    ADD CONSTRAINT fk_gastos_fatura_metodo
        FOREIGN KEY (fatura_id, metodo_pagamento_id)
        REFERENCES faturas (id, metodo_pagamento_id),
    ADD CONSTRAINT ck_gastos_fatura_por_tipo CHECK (
        (tipo_metodo_pagamento = 'CREDITO' AND fatura_id IS NOT NULL)
        OR (tipo_metodo_pagamento IN ('DEBITO', 'PIX') AND fatura_id IS NULL)
    ),
    ADD CONSTRAINT ck_gastos_tipo_metodo CHECK (tipo_metodo_pagamento IN ('CREDITO', 'DEBITO', 'PIX'));

CREATE INDEX idx_gastos_metodo_pagamento_id ON gastos (metodo_pagamento_id);
CREATE INDEX idx_gastos_fatura_id ON gastos (fatura_id);
