ALTER TABLE gastos
    DROP CONSTRAINT ck_gastos_data_parcela_primeiro_dia,
    ADD CONSTRAINT ck_gastos_data_parcela_primeiro_dia
        CHECK (compra_parcelada_id IS NULL OR numero_parcela = 1 OR EXTRACT(DAY FROM data) = 1);
