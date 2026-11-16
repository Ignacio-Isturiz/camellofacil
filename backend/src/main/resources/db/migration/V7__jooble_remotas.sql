-- Jooble se usa con su servidor internacional y solo para prácticas remotas, que se publican en inglés:
-- las búsquedas pasan a inglés (con ubicación «Remote» en la petición). Mismos pesos por carrera.
UPDATE consulta_vacantes SET texto = 'software engineering intern'    WHERE fuente = 'jooble' AND carrera_codigo = 'informatica';
UPDATE consulta_vacantes SET texto = 'industrial engineering intern'  WHERE fuente = 'jooble' AND carrera_codigo = 'industrial';
UPDATE consulta_vacantes SET texto = 'data analyst intern'            WHERE fuente = 'jooble' AND carrera_codigo = 'datos';
UPDATE consulta_vacantes SET texto = 'economics intern'               WHERE fuente = 'jooble' AND carrera_codigo = 'economia';
UPDATE consulta_vacantes SET texto = 'accounting intern'              WHERE fuente = 'jooble' AND carrera_codigo = 'contaduria';
UPDATE consulta_vacantes SET texto = 'business administration intern' WHERE fuente = 'jooble' AND carrera_codigo = 'administracion';
UPDATE consulta_vacantes SET texto = 'legal intern'                   WHERE fuente = 'jooble' AND carrera_codigo = 'derecho';
UPDATE consulta_vacantes SET texto = 'education intern'               WHERE fuente = 'jooble' AND carrera_codigo = 'sociales';

-- La llave anterior apuntaba al servidor de Colombia y quedó pausada por el 403: se quita la pausa.
UPDATE uso_fuente SET pausada_hasta = NULL WHERE fuente = 'jooble';
