-- Personas que inician sesión (por ahora solo con Microsoft). Identificadas por el id estable del proveedor.
CREATE TABLE usuario (
    id            UUID         PRIMARY KEY,
    proveedor     VARCHAR(20)  NOT NULL,
    id_externo    VARCHAR(100) NOT NULL,
    correo        VARCHAR(320) NOT NULL,
    nombre        VARCHAR(200) NOT NULL,
    creado_en     TIMESTAMPTZ  NOT NULL,
    ultimo_acceso TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_usuario_proveedor UNIQUE (proveedor, id_externo),
    CONSTRAINT uq_usuario_correo UNIQUE (correo)
);
