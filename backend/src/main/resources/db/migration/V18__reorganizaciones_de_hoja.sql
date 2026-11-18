-- Reorganizar la hoja de vida a otro formato (Harvard o ATS). La propuesta es un plan de cambios que la persona
-- aplica o revierte uno por uno; el documento se arma al leerla, así que alternar un cambio no llama a la IA.
CREATE TABLE reorganizacion_hoja (
    id            UUID         PRIMARY KEY,
    usuario_id    UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    formato       VARCHAR(10)  NOT NULL,
    idioma        VARCHAR(2)   NOT NULL,
    -- Huella de la hoja base con que se hizo: si no cambió, se reutiliza la redacción de la IA (cambiar de formato
    -- o de idioma no cuesta otra llamada).
    base_huella   VARCHAR(64)  NOT NULL,
    -- La hoja base tal como estaba al proponer: lo que se compara no cambia aunque la persona suba otra después.
    base          JSONB        NOT NULL,
    -- Redacción ya verificada (perfil y descripciones) y si salió de una llamada a la IA.
    ia            JSONB        NOT NULL,
    ia_generada   BOOLEAN      NOT NULL,
    plan          JSONB        NOT NULL,
    avisos        JSONB        NOT NULL,
    version_id    UUID         REFERENCES version_hoja (id) ON DELETE SET NULL,
    creada_en     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_reorg_formato CHECK (formato IN ('harvard', 'ats')),
    CONSTRAINT ck_reorg_idioma CHECK (idioma IN ('es', 'en'))
);

CREATE INDEX ix_reorg_usuario ON reorganizacion_hoja (usuario_id, creada_en DESC);
