CREATE TABLE grupos_planejados (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);

CREATE TABLE itens_grupos_planejados (
    grupo_planejado_id UUID NOT NULL,
    posicao INTEGER NOT NULL,
    nome VARCHAR(255) NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    categoria VARCHAR(255) NOT NULL,
    descricao TEXT,
    CONSTRAINT pk_itens_grupos_planejados PRIMARY KEY (grupo_planejado_id, posicao),
    CONSTRAINT fk_itens_grupos_planejados_grupo
        FOREIGN KEY (grupo_planejado_id) REFERENCES grupos_planejados (id) ON DELETE CASCADE
);
