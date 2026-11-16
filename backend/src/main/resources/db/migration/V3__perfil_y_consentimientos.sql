-- Carreras que la persona puede elegir (RF-06). Las gestionará el administrador; aquí se cargan las iniciales.
CREATE TABLE carrera (
    codigo VARCHAR(40)  PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    activa BOOLEAN      NOT NULL DEFAULT TRUE,
    orden  SMALLINT     NOT NULL
);

INSERT INTO carrera (codigo, nombre, orden) VALUES
    ('derecho',        'Derecho', 1),
    ('contaduria',     'Contaduría Pública', 2),
    ('administracion', 'Administración de Empresas', 3),
    ('economia',       'Economía', 4),
    ('informatica',    'Ingeniería Informática', 5),
    ('industrial',     'Ingeniería Industrial', 6),
    ('datos',          'Ingeniería en Ciencia de Datos e Inteligencia de Negocios', 7),
    ('sociales',       'Licenciatura en Ciencias Sociales', 8);

-- Perfil de la persona (RF-05 a RF-08). Se crea al completar el modal del primer ingreso.
CREATE TABLE perfil (
    usuario_id             UUID         PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    tipo                   VARCHAR(10)  NOT NULL,
    carrera_codigo         VARCHAR(40)  NOT NULL REFERENCES carrera (codigo),
    semestre               SMALLINT,
    ciudad                 VARCHAR(100),
    modalidades            VARCHAR(40),
    fecha_inicio_practica  DATE,
    disponibilidad         VARCHAR(20),
    creado_en              TIMESTAMPTZ  NOT NULL,
    actualizado_en         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_perfil_tipo CHECK (tipo IN ('ESTUDIANTE', 'EGRESADO')),
    CONSTRAINT ck_perfil_semestre CHECK (
        (tipo = 'ESTUDIANTE' AND semestre BETWEEN 1 AND 10) OR (tipo = 'EGRESADO' AND semestre IS NULL)),
    CONSTRAINT ck_perfil_disponibilidad CHECK (
        disponibilidad IS NULL OR disponibilidad IN ('TIEMPO_COMPLETO', 'MEDIO_TIEMPO', 'FLEXIBLE'))
);

-- Consentimientos (Ley 1581, RF-92/93): qué aceptó cada persona, en qué versión y cuándo. Revocables.
CREATE TABLE consentimiento (
    id           UUID        PRIMARY KEY,
    usuario_id   UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo         VARCHAR(30) NOT NULL,
    version      VARCHAR(10) NOT NULL,
    otorgado_en  TIMESTAMPTZ NOT NULL,
    revocado_en  TIMESTAMPTZ,
    CONSTRAINT ck_consentimiento_tipo CHECK (tipo IN ('TERMINOS_PRIVACIDAD', 'ANALISIS_IA'))
);

CREATE INDEX ix_consentimiento_usuario_tipo ON consentimiento (usuario_id, tipo);
