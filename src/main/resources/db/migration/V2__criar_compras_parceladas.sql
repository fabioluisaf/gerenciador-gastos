CREATE TABLE compras_parceladas (
    id UUID PRIMARY KEY,
    valor_total NUMERIC(19, 2) NOT NULL
);

ALTER TABLE gastos
    ADD COLUMN compra_parcelada_id UUID,
    ADD COLUMN numero_parcela INTEGER,
    ADD CONSTRAINT fk_gastos_compra_parcelada
        FOREIGN KEY (compra_parcelada_id) REFERENCES compras_parceladas (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_gastos_numero_parcela CHECK (
        (compra_parcelada_id IS NULL AND numero_parcela IS NULL)
        OR (compra_parcelada_id IS NOT NULL AND numero_parcela IS NOT NULL AND numero_parcela > 0)
    ),
    ADD CONSTRAINT uq_gastos_compra_numero_parcela UNIQUE (compra_parcelada_id, numero_parcela),
    ADD CONSTRAINT ck_gastos_data_parcela_primeiro_dia
        CHECK (compra_parcelada_id IS NULL OR EXTRACT(DAY FROM data) = 1);

CREATE INDEX idx_gastos_compra_parcelada_id ON gastos (compra_parcelada_id);
