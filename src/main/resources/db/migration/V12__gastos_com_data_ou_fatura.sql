ALTER TABLE gastos
    ADD CONSTRAINT ck_gastos_data_ou_fatura CHECK (data IS NOT NULL OR fatura_id IS NOT NULL);
