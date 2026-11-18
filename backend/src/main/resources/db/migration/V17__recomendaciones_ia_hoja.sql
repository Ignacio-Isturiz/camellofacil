-- Recomendaciones de la IA sobre la hoja base (se suman a las observaciones de reglas). Una fila por persona:
-- vale mientras la hoja no cambie (hoja_actualizada_en); si la persona corrige o reemplaza su hoja, se vuelven a pedir.
CREATE TABLE recomendacion_hoja_ia (
    usuario_id            UUID        PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    hoja_actualizada_en   TIMESTAMPTZ NOT NULL,
    items                 JSONB       NOT NULL,
    -- Tope de generaciones por persona y día (el día es el de Colombia).
    dia                   DATE        NOT NULL,
    intentos_hoy          INTEGER     NOT NULL,
    creada_en             TIMESTAMPTZ NOT NULL
);
