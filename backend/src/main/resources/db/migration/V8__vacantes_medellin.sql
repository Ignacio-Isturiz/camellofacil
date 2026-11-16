-- La universidad está en Medellín: solo sirven vacantes remotas o presenciales/híbridas del área metropolitana.

-- 1. Las búsquedas de JSearch (Colombia) apuntan a Medellín, para no gastar el cupo en ofertas de otras ciudades.
UPDATE consulta_vacantes SET texto = texto || ' en Medellín' WHERE fuente = 'jsearch' AND texto NOT LIKE '%Medellín%';

-- 2. Se cierran las ya guardadas que no cumplen (de la primera ronda, casi todas de Bogotá). Mismo criterio que
--    Clasificador.enAreaMetropolitana: municipio del área metropolitana y, si dice región, Antioquia o Colombia.
UPDATE vacante SET cerrada_en = now()
WHERE cerrada_en IS NULL
  AND modalidad <> 'REMOTO'
  AND NOT (
        coalesce(ciudad, '') ~* '^\s*(medell[ií]n|envigado|itag[uü][ií]|sabaneta|bello|la estrella|caldas|copacabana|girardota|barbosa|valle de aburr[aá])\s*(,\s*(antioquia|ant\.?|colombia|antioquia,\s*colombia|[aá]rea metropolitana)\s*)?$'
  );

-- 3. Ya no existe «ocultar vacante».
DELETE FROM vacante_marca WHERE tipo = 'OCULTA';
ALTER TABLE vacante_marca DROP CONSTRAINT ck_marca_tipo;
ALTER TABLE vacante_marca ADD CONSTRAINT ck_marca_tipo CHECK (tipo = 'GUARDADA');
