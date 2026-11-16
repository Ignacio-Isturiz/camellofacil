-- Hoja de vida base de cada persona (RF-13 a RF-16). Una por persona: subir otra la reemplaza.
-- El estado ANALIZANDO es también la cola del análisis en segundo plano (sin estado en memoria).
CREATE TABLE hoja_de_vida (
    usuario_id            UUID         PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    nombre_archivo        VARCHAR(255) NOT NULL,
    tipo_archivo          VARCHAR(4)   NOT NULL,
    tamano_bytes          INTEGER      NOT NULL,
    estado                VARCHAR(15)  NOT NULL,
    idioma                VARCHAR(2),
    paginas               SMALLINT,
    -- Datos extraídos (y luego corregidos por la persona), en JSON.
    datos                 JSONB,
    intentos              SMALLINT     NOT NULL DEFAULT 0,
    siguiente_intento_en  TIMESTAMPTZ,
    subida_en             TIMESTAMPTZ  NOT NULL,
    actualizada_en        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_hoja_tipo CHECK (tipo_archivo IN ('PDF', 'DOCX', 'DOC')),
    CONSTRAINT ck_hoja_estado CHECK (estado IN ('ANALIZANDO', 'SIN_CONFIRMAR', 'CONFIRMADA', 'ILEGIBLE', 'ERROR')),
    CONSTRAINT ck_hoja_idioma CHECK (idioma IS NULL OR idioma IN ('es', 'en'))
);

CREATE INDEX ix_hoja_pendiente ON hoja_de_vida (siguiente_intento_en) WHERE estado = 'ANALIZANDO';

-- El archivo original va aparte para no cargar sus bytes cada vez que se consulta el estado.
CREATE TABLE hoja_de_vida_archivo (
    usuario_id  UUID  PRIMARY KEY REFERENCES hoja_de_vida (usuario_id) ON DELETE CASCADE,
    contenido   BYTEA NOT NULL
);
