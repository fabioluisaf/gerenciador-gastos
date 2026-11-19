ALTER TABLE instituicoes_financeiras
    ADD COLUMN cor VARCHAR(7),
    ADD CONSTRAINT ck_instituicoes_financeiras_cor CHECK (cor ~ '^#[0-9A-F]{6}$');

UPDATE instituicoes_financeiras SET cor = '#820AD1' WHERE LOWER(nome) = 'nubank';
UPDATE instituicoes_financeiras SET cor = '#005CA9' WHERE LOWER(nome) = 'caixa';
