-- Búsquedas de JSearch más generales y con cargos afines a cada carrera de UNAULA (no solo el nombre exacto).
-- El peso de cada búsqueda reparte la prioridad de su carrera:
--   Ingeniería Informática 15 (5 × 3) > Industrial, Ciencia de Datos, Economía 12 (3 × 4)
--   > Contaduría, Administración 6 (3 × 2) > Derecho, Licenciatura en Ciencias Sociales 3 (3 × 1).

-- Las que ya existen: se ajusta el peso. La de sistemas corrió antes de apuntar a Medellín: vuelve a la fila.
UPDATE consulta_vacantes SET peso = 3, ultima_ejecucion = NULL
WHERE fuente = 'jsearch' AND texto = 'practicante ingeniería de sistemas en Medellín';
UPDATE consulta_vacantes SET peso = 3 WHERE fuente = 'jsearch' AND texto = 'practicante desarrollo de software en Medellín';
UPDATE consulta_vacantes SET peso = 4 WHERE fuente = 'jsearch' AND carrera_codigo IN ('industrial', 'datos', 'economia');
UPDATE consulta_vacantes SET peso = 2 WHERE fuente = 'jsearch' AND carrera_codigo IN ('contaduria', 'administracion');
UPDATE consulta_vacantes SET peso = 1 WHERE fuente = 'jsearch' AND carrera_codigo IN ('derecho', 'sociales');
-- «licenciatura ciencias sociales» no trajo nada: se cambia por algo más general.
UPDATE consulta_vacantes SET texto = 'practicante docente en Medellín', ultima_ejecucion = NULL
WHERE fuente = 'jsearch' AND texto = 'practicante licenciatura ciencias sociales en Medellín';

INSERT INTO consulta_vacantes (id, fuente, carrera_codigo, texto, peso) VALUES
    (gen_random_uuid(), 'jsearch', 'informatica',    'practicante ingeniería informática en Medellín',      3),
    (gen_random_uuid(), 'jsearch', 'informatica',    'aprendiz desarrollador en Medellín',                  3),
    (gen_random_uuid(), 'jsearch', 'informatica',    'practicante tecnología e informática en Medellín',    3),
    (gen_random_uuid(), 'jsearch', 'industrial',     'practicante logística en Medellín',                   4),
    (gen_random_uuid(), 'jsearch', 'industrial',     'practicante producción y calidad en Medellín',        4),
    (gen_random_uuid(), 'jsearch', 'datos',          'practicante inteligencia de negocios en Medellín',    4),
    (gen_random_uuid(), 'jsearch', 'economia',       'practicante finanzas en Medellín',                    4),
    (gen_random_uuid(), 'jsearch', 'economia',       'practicante análisis financiero en Medellín',         4),
    (gen_random_uuid(), 'jsearch', 'contaduria',     'aprendiz contable en Medellín',                       2),
    (gen_random_uuid(), 'jsearch', 'contaduria',     'practicante auditoría en Medellín',                   2),
    (gen_random_uuid(), 'jsearch', 'administracion', 'practicante talento humano en Medellín',              2),
    (gen_random_uuid(), 'jsearch', 'administracion', 'practicante comercial y mercadeo en Medellín',        2),
    (gen_random_uuid(), 'jsearch', 'derecho',        'practicante jurídico en Medellín',                    1),
    (gen_random_uuid(), 'jsearch', 'derecho',        'judicante en Medellín',                               1),
    (gen_random_uuid(), 'jsearch', 'sociales',       'practicante educación y pedagogía en Medellín',       1),
    (gen_random_uuid(), 'jsearch', 'sociales',       'practicante ciencias sociales en Medellín',           1);
