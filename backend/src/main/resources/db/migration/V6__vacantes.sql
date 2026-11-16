-- Vacantes traídas de fuentes externas (RF-30 a RF-34). Las fuentes se consultan con un cupo controlado
-- (uso_fuente) y siguiendo una lista de búsquedas con prioridad por carrera (consulta_vacantes).

CREATE TABLE vacante (
    id                 UUID          PRIMARY KEY,
    fuente             VARCHAR(20)   NOT NULL,
    id_externo         VARCHAR(600)  NOT NULL,
    -- Cargo + empresa + ciudad normalizados (SHA-256): la misma oferta publicada en varios portales es una sola (RF-33).
    huella             CHAR(64)      NOT NULL UNIQUE,
    cargo              VARCHAR(300)  NOT NULL,
    empresa            VARCHAR(200)  NOT NULL,
    ciudad             VARCHAR(120),
    modalidad          VARCHAR(10)   NOT NULL,
    tipo               VARCHAR(12)   NOT NULL,
    publicador         VARCHAR(120),
    url_postulacion    VARCHAR(2000) NOT NULL,
    descripcion        TEXT,
    responsabilidades  JSONB         NOT NULL DEFAULT '[]',
    requisitos         JSONB         NOT NULL DEFAULT '[]',
    carreras           TEXT[]        NOT NULL DEFAULT '{}',
    publicada_en       TIMESTAMPTZ   NOT NULL,
    vista_en           TIMESTAMPTZ   NOT NULL,
    creada_en          TIMESTAMPTZ   NOT NULL,
    cerrada_en         TIMESTAMPTZ,
    CONSTRAINT ck_vacante_modalidad CHECK (modalidad IN ('PRESENCIAL', 'HIBRIDO', 'REMOTO')),
    CONSTRAINT ck_vacante_tipo CHECK (tipo IN ('PRACTICA', 'PROFESIONAL'))
);

CREATE INDEX ix_vacante_abiertas ON vacante (publicada_en DESC) WHERE cerrada_en IS NULL;

-- Búsquedas que se lanzan contra cada fuente. El peso da la prioridad: una búsqueda de peso 5 se repite
-- cinco veces más seguido que una de peso 1.
CREATE TABLE consulta_vacantes (
    id                UUID          PRIMARY KEY,
    fuente            VARCHAR(20)   NOT NULL,
    carrera_codigo    VARCHAR(30)   NOT NULL,
    texto             VARCHAR(200)  NOT NULL,
    peso              SMALLINT      NOT NULL,
    activa            BOOLEAN       NOT NULL DEFAULT TRUE,
    ultima_ejecucion  TIMESTAMPTZ,
    CONSTRAINT ck_consulta_peso CHECK (peso BETWEEN 1 AND 10),
    CONSTRAINT uq_consulta UNIQUE (fuente, texto)
);

-- Cupo de cada fuente: cuántas peticiones lleva en el periodo actual (30 días JSearch, 1 día Jooble).
-- Se bloquea con SELECT ... FOR UPDATE para que dos copias del servidor no se pasen del límite.
CREATE TABLE uso_fuente (
    fuente          VARCHAR(20)  PRIMARY KEY,
    periodo_inicio  TIMESTAMPTZ  NOT NULL,
    consumidas      INTEGER      NOT NULL DEFAULT 0,
    pausada_hasta   TIMESTAMPTZ
);

-- Vacantes que cada persona guarda u oculta.
CREATE TABLE vacante_marca (
    usuario_id  UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    vacante_id  UUID         NOT NULL REFERENCES vacante (id) ON DELETE CASCADE,
    tipo        VARCHAR(10)  NOT NULL,
    creada_en   TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (usuario_id, vacante_id, tipo),
    CONSTRAINT ck_marca_tipo CHECK (tipo IN ('GUARDADA', 'OCULTA'))
);

-- Prioridad por carrera: informática > industrial = ciencia de datos = economía > contaduría = administración
-- > derecho = licenciatura en ciencias sociales.
INSERT INTO consulta_vacantes (id, fuente, carrera_codigo, texto, peso) VALUES
    (gen_random_uuid(), 'jsearch', 'informatica',    'practicante ingeniería de sistemas',         5),
    (gen_random_uuid(), 'jsearch', 'industrial',     'practicante ingeniería industrial',          4),
    (gen_random_uuid(), 'jsearch', 'datos',          'practicante análisis de datos',              4),
    (gen_random_uuid(), 'jsearch', 'economia',       'practicante economía',                       4),
    (gen_random_uuid(), 'jsearch', 'contaduria',     'practicante contaduría pública',             2),
    (gen_random_uuid(), 'jsearch', 'administracion', 'practicante administración de empresas',     2),
    (gen_random_uuid(), 'jsearch', 'derecho',        'practicante derecho',                        1),
    (gen_random_uuid(), 'jsearch', 'sociales',       'practicante licenciatura ciencias sociales', 1),
    (gen_random_uuid(), 'jooble',  'informatica',    'practicante sistemas',                       5),
    (gen_random_uuid(), 'jooble',  'industrial',     'practicante ingeniería industrial',          4),
    (gen_random_uuid(), 'jooble',  'datos',          'practicante datos',                          4),
    (gen_random_uuid(), 'jooble',  'economia',       'practicante economía',                       4),
    (gen_random_uuid(), 'jooble',  'contaduria',     'practicante contable',                       2),
    (gen_random_uuid(), 'jooble',  'administracion', 'practicante administración',                 2),
    (gen_random_uuid(), 'jooble',  'derecho',        'practicante derecho',                        1),
    (gen_random_uuid(), 'jooble',  'sociales',       'practicante ciencias sociales',              1);
