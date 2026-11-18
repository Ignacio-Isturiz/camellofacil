-- Adaptación de la hoja de vida a una vacante. Una por persona y vacante: regenerarla reemplaza la anterior.
-- Se guardan los dos documentos ya armados (la base y la adaptada) para que lo que la persona revisó no cambie
-- aunque después edite su hoja base.
CREATE TABLE adaptacion_hoja (
    id            UUID         PRIMARY KEY,
    usuario_id    UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    vacante_id    UUID         NOT NULL REFERENCES vacante (id) ON DELETE CASCADE,
    plantilla_id  VARCHAR(40)  NOT NULL,
    idioma        VARCHAR(2)   NOT NULL,
    valores       JSONB        NOT NULL,
    original      JSONB        NOT NULL,
    adaptada      JSONB        NOT NULL,
    cambios       JSONB        NOT NULL,
    avisos        JSONB        NOT NULL,
    -- La versión en que quedó guardada al aceptarla (si ya se aceptó).
    version_id    UUID         REFERENCES version_hoja (id) ON DELETE SET NULL,
    creada_en     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_adaptacion UNIQUE (usuario_id, vacante_id),
    CONSTRAINT ck_adaptacion_idioma CHECK (idioma IN ('es', 'en'))
);

-- Adaptaciones gastadas por persona y día. El contador se sube con un solo INSERT ... ON CONFLICT, así que dos
-- copias del servidor no pueden pasarse del límite.
CREATE TABLE uso_adaptacion (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID    NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    fecha       DATE    NOT NULL,
    usadas      INTEGER NOT NULL,
    CONSTRAINT ux_uso_adaptacion UNIQUE (usuario_id, fecha)
);
