CREATE TABLE gastos (
    id UUID PRIMARY KEY,
    data DATE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    categoria VARCHAR(255) NOT NULL,
    metodo_pagamento VARCHAR(255) NOT NULL,
    descricao TEXT,
    realizado BOOLEAN NOT NULL
);
