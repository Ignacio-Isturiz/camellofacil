-- Mis postulaciones (RF-40): las vacantes en las que la persona dio clic en «Postular». La postulación termina
-- en la página de la empresa; aquí solo queda el historial de clics. Se guarda una copia de cargo, empresa y
-- lugar para que el historial no dependa de cómo cambie la vacante.

CREATE TABLE postulacion (
    id               UUID          PRIMARY KEY,
    usuario_id       UUID          NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    vacante_id       UUID          NOT NULL REFERENCES vacante (id) ON DELETE CASCADE,
    cargo            VARCHAR(300)  NOT NULL,
    empresa          VARCHAR(200)  NOT NULL,
    ciudad           VARCHAR(120),
    modalidad        VARCHAR(10)   NOT NULL,
    tipo             VARCHAR(12)   NOT NULL,
    hoja_usada       VARCHAR(10)   NOT NULL,
    clics            INTEGER       NOT NULL DEFAULT 1,
    primer_clic_en   TIMESTAMPTZ   NOT NULL,
    ultimo_clic_en   TIMESTAMPTZ   NOT NULL,
    -- «Quitar del historial» con opción de deshacer: se marca y un proceso la borra del todo al día siguiente.
    quitada_en       TIMESTAMPTZ,
    CONSTRAINT uq_postulacion UNIQUE (usuario_id, vacante_id),
    CONSTRAINT ck_postulacion_modalidad CHECK (modalidad IN ('PRESENCIAL', 'HIBRIDO', 'REMOTO')),
    CONSTRAINT ck_postulacion_tipo CHECK (tipo IN ('PRACTICA', 'PROFESIONAL')),
    CONSTRAINT ck_postulacion_hoja CHECK (hoja_usada IN ('BASE', 'ADAPTADA')),
    CONSTRAINT ck_postulacion_clics CHECK (clics >= 1)
);

CREATE INDEX ix_postulacion_quitada ON postulacion (quitada_en) WHERE quitada_en IS NOT NULL;

-- Cada clic, para el resumen «N clics en postular · últimos 90 días».
CREATE TABLE clic_postulacion (
    id              UUID         PRIMARY KEY,
    postulacion_id  UUID         NOT NULL REFERENCES postulacion (id) ON DELETE CASCADE,
    creado_en       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_clic_postulacion ON clic_postulacion (postulacion_id, creado_en);
