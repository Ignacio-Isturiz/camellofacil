-- «¿Te sirvió?» en las recomendadas de Inicio. Una respuesta por persona y vacante (la última cuenta);
-- las que la persona marcó con «No» ya no se le recomiendan.
CREATE TABLE voto_recomendacion (
    usuario_id  UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    vacante_id  UUID         NOT NULL REFERENCES vacante (id) ON DELETE CASCADE,
    sirvio      BOOLEAN      NOT NULL,
    creado_en   TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (usuario_id, vacante_id)
);
