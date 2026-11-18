-- Versiones de la hoja de vida (RF de versiones) y borradores del editor.
-- La versión guarda el documento ya armado (lo que se ve y se descarga) y, cuando existe, los valores con los
-- que se armó, para poder volver a editarla.
CREATE TABLE version_hoja (
    id            UUID         PRIMARY KEY,
    usuario_id    UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nombre        VARCHAR(120) NOT NULL,
    tipo          VARCHAR(15)  NOT NULL,
    idioma        VARCHAR(2)   NOT NULL,
    plantilla_id  VARCHAR(40)  NOT NULL,
    es_base       BOOLEAN      NOT NULL DEFAULT FALSE,
    nota          VARCHAR(255),
    documento     JSONB        NOT NULL,
    valores       JSONB,
    creada_en     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_version_tipo CHECK (tipo IN ('BASE', 'REORGANIZADA', 'ADAPTADA')),
    CONSTRAINT ck_version_idioma CHECK (idioma IN ('es', 'en'))
);

CREATE INDEX ix_version_usuario ON version_hoja (usuario_id, creada_en DESC);
-- Una sola versión base por persona.
CREATE UNIQUE INDEX ux_version_base ON version_hoja (usuario_id) WHERE es_base;

CREATE TABLE borrador_hoja (
    id            UUID         PRIMARY KEY,
    usuario_id    UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    plantilla_id  VARCHAR(40)  NOT NULL,
    idioma        VARCHAR(2)   NOT NULL,
    valores       JSONB        NOT NULL,
    guardado_en   TIMESTAMPTZ,
    creado_en     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_borrador_idioma CHECK (idioma IN ('es', 'en'))
);

CREATE INDEX ix_borrador_usuario ON borrador_hoja (usuario_id, creado_en);
