-- Más búsquedas para las carreras principales, para que llamados seguidos (la carga inicial) no repitan la
-- misma búsqueda. El peso de la carrera se reparte entre sus búsquedas: informática sigue sumando 5 y
-- ciencia de datos 4.
UPDATE consulta_vacantes SET peso = 3
WHERE fuente = 'jsearch' AND carrera_codigo = 'informatica' AND texto = 'practicante ingeniería de sistemas en Medellín';
UPDATE consulta_vacantes SET peso = 2
WHERE fuente = 'jsearch' AND carrera_codigo = 'datos' AND texto = 'practicante análisis de datos en Medellín';

INSERT INTO consulta_vacantes (id, fuente, carrera_codigo, texto, peso) VALUES
    (gen_random_uuid(), 'jsearch', 'informatica', 'practicante desarrollo de software en Medellín', 2),
    (gen_random_uuid(), 'jsearch', 'datos',       'practicante ciencia de datos en Medellín',       2);
