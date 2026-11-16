ALTER TABLE gastos
    DROP CONSTRAINT ck_gastos_data_parcela_primeiro_dia;

UPDATE gastos parcela
SET data = primeira.data
FROM gastos primeira
WHERE primeira.compra_parcelada_id = parcela.compra_parcelada_id
    AND primeira.numero_parcela = 1
    AND parcela.data IS DISTINCT FROM primeira.data;

DO $$
BEGIN
    IF EXISTS (
        SELECT compra_parcelada_id
        FROM gastos
        WHERE compra_parcelada_id IS NOT NULL
        GROUP BY compra_parcelada_id
        HAVING COUNT(DISTINCT data) > 1
            OR (COUNT(data) > 0 AND COUNT(data) < COUNT(*))
    ) THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_gastos_datas_parcelas_iguais',
            MESSAGE = 'DATAS_PARCELAS_DIVERGENTES';
    END IF;
END;
$$;

CREATE FUNCTION validar_datas_parcelas_iguais()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    data_referencia DATE;
BEGIN
    IF NEW.compra_parcelada_id IS NULL THEN
        RETURN NULL;
    END IF;

    PERFORM id
    FROM compras_parceladas
    WHERE id = NEW.compra_parcelada_id
    FOR NO KEY UPDATE;

    SELECT data INTO data_referencia
    FROM gastos
    WHERE compra_parcelada_id = NEW.compra_parcelada_id
    ORDER BY numero_parcela
    LIMIT 1;

    IF EXISTS (
        SELECT 1
        FROM gastos
        WHERE compra_parcelada_id = NEW.compra_parcelada_id
            AND data IS DISTINCT FROM data_referencia
    ) THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_gastos_datas_parcelas_iguais',
            MESSAGE = 'DATAS_PARCELAS_DIVERGENTES';
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ck_gastos_datas_parcelas_iguais
    AFTER INSERT OR UPDATE OF data, compra_parcelada_id ON gastos
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION validar_datas_parcelas_iguais();
