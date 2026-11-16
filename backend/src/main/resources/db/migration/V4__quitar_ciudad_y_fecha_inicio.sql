-- Se simplifican las preferencias del perfil: ya no se piden la ciudad ni la fecha desde la que se busca.
ALTER TABLE perfil DROP COLUMN ciudad;
ALTER TABLE perfil DROP COLUMN fecha_inicio_practica;
