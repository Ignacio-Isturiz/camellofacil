-- «Desarrollo» solo es informática si es de software, web o aplicaciones. Se corrigen las vacantes que
-- quedaron marcadas como informática por «Desarrollo Comercial» o «Desarrollo y Talento Humano».
UPDATE vacante SET carreras = array_remove(carreras, 'informatica')
WHERE 'informatica' = ANY (carreras)
  AND cargo ~* 'desarrollo\s+(comercial|y\s+talento|humano|organizacional|de\s+negocios?)';
