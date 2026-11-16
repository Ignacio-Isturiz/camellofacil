package com.camellofacil.backend.vacantes.app;

import com.camellofacil.backend.vacantes.app.ModelosVacante.Modalidad;
import com.camellofacil.backend.vacantes.app.ModelosVacante.TipoVacante;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteNueva;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Convierte una oferta externa en una vacante lista para guardar: limpia y recorta los textos, descarta lo
 * inválido (sin cargo, sin empresa, enlace que no es http/https), la clasifica en práctica o empleo (RF-31),
 * le asigna carreras (RF-32) y calcula su huella para detectar duplicados (RF-33). Sin estado: funciones puras.
 */
public final class Clasificador {

    static final int MAXIMO_DESCRIPCION = 20_000;
    static final int MAXIMO_PUNTOS = 15;

    private static final Pattern PRACTICA = Pattern.compile(
            "\\b(practicante|practicas?|pasante|pasantia|aprendiz|aprendizaje|intern|internship|becari[oa]|judicante|judicatura|estudiante)\\b");
    private static final Pattern TIPO_PRACTICA = Pattern.compile("\\b(intern|pasantia|practicas?|aprendiz)");
    private static final Pattern REMOTO = Pattern.compile("\\b(remoto|remota|teletrabajo|remote|home office|trabajo desde casa)\\b");
    private static final Pattern HIBRIDO = Pattern.compile("\\b(hibrido|hibrida|hybrid)\\b");
    private static final Pattern ETIQUETAS_HTML = Pattern.compile("<[^>]{0,500}>");

    /** Palabras del cargo que indican a qué carreras sirve la vacante (texto ya normalizado). */
    private static final Map<String, Pattern> CARRERAS_POR_CARGO = new LinkedHashMap<>();

    /*
     * Carreras de UNAULA y sus afines. Una vacante puede servir a varias (p. ej. «Power BI» es de datos y de
     * informática; «costos» es de contaduría y de economía).
     */
    static {
        // Ingeniería Informática
        CARRERAS_POR_CARGO.put("informatica", Pattern.compile(
                "\\b(sistemas|software|desarrollador\\w*|desarrollo (de )?(software|web|aplicaciones|sistemas)|developer|programa(dor|dora|cion)\\w*|informatic\\w*|ti|tic|tecnologia\\w*"
                        + "|ciberseguridad|seguridad informatica|redes(?! sociales)|infraestructura|soporte tecnico|mesa de ayuda|help desk|qa"
                        + "|devops|cloud|nube|frontend|backend|full ?stack|web|automatizacion|rpa|inteligencia artificial|ia"
                        + "|power bi|it services|it support|information technology|cybersecurity|computer science)\\b"));
        // Ingeniería Industrial
        CARRERAS_POR_CARGO.put("industrial", Pattern.compile(
                "\\b(industrial\\w*|produccion|logistic\\w*|procesos|calidad|operaciones|cadena de suministro|manufactura"
                        + "|compras|abastecimiento|inventario\\w*|almacen\\w*|mejora continua|lean|planeacion|mantenimiento"
                        + "|seguridad y salud|sst|supply chain|operations|manufacturing|quality|procurement)\\b"));
        // Ingeniería en Ciencia de Datos (e inteligencia de negocios)
        CARRERAS_POR_CARGO.put("datos", Pattern.compile(
                "\\b(datos|data|analitica|analytics|bi|business intelligence|inteligencia de negocios|power bi|tableau"
                        + "|dashboards?|tableros?|reportes?|indicadores|sql|python|machine learning|estadistic\\w*"
                        + "|cientific\\w* de datos|big data|inteligencia artificial|ia)\\b"));
        // Economía
        CARRERAS_POR_CARGO.put("economia", Pattern.compile(
                "\\b(economia|economista|economic\\w*|economist|financier\\w*|finanzas|finance|financial|banca|bancari\\w*"
                        + "|riesgos?|credito|inversion\\w*|tesoreria|costos|presupuesto\\w*|investigacion economica|mercados)\\b"));
        // Contaduría Pública
        CARRERAS_POR_CARGO.put("contaduria", Pattern.compile(
                "\\b(contad\\w*|contable|contabilidad|auditor\\w*|tributari\\w*|impuestos|revisoria|costos|tesoreria|nomina"
                        + "|facturacion|cartera|conciliacion\\w*|accounting|accountant|audit\\w*|tax)\\b"));
        // Administración de Empresas
        CARRERAS_POR_CARGO.put("administracion", Pattern.compile(
                "\\b(administracion de empresas|administrador de empresas|administrativ\\w*|gestion empresarial"
                        + "|talento humano|recursos humanos|gestion humana|seleccion|bienestar|mercadeo|comercial|ventas"
                        + "|servicio al cliente|proyectos|emprendimiento|business administration|human resources|marketing)\\b"));
        // Derecho
        CARRERAS_POR_CARGO.put("derecho", Pattern.compile(
                "\\b(derecho|juridic\\w*|abogad\\w*|legal|judicante|judicatura|contratacion|cumplimiento|compliance"
                        + "|litigio\\w*|notari\\w*|law|paralegal)\\b"));
        // Licenciatura en Ciencias Sociales
        CARRERAS_POR_CARGO.put("sociales", Pattern.compile(
                "\\b(ciencias sociales|trabajo social|gestion social|docente|docencia|profesor\\w*|licenciad\\w*|licenciatura|pedagog\\w*"
                        + "|educacion|educativ\\w*|tutor\\w*|comunitari\\w*|investigacion social"
                        + "|social sciences?|teaching|teacher|education)\\b"));
    }

    private Clasificador() {}

    /** Vacía si la oferta no sirve (sin cargo, sin empresa o sin un enlace http/https válido). */
    public static Optional<VacanteNueva> clasificar(String fuente, String carreraConsulta, VacanteExterna e, java.time.Instant ahora) {
        String cargo = linea(e.cargo(), 300);
        String empresa = linea(e.empresa(), 200);
        String url = urlSegura(e.url());
        String idExterno = linea(e.idExterno(), 600);
        if (cargo == null || empresa == null || url == null || idExterno == null) return Optional.empty();

        String ciudad = linea(e.ciudad(), 120);
        String descripcion = texto(e.descripcion());
        String cargoNormal = normalizar(cargo);
        String todo = cargoNormal + " " + normalizar(Objects.requireNonNullElse(descripcion, ""));

        TipoVacante tipo = PRACTICA.matcher(cargoNormal).find()
                        || TIPO_PRACTICA.matcher(normalizar(Objects.requireNonNullElse(e.tipoEmpleo(), ""))).find()
                ? TipoVacante.PRACTICA
                : TipoVacante.PROFESIONAL;

        Modalidad modalidad = e.remota() || REMOTO.matcher(cargoNormal).find()
                ? Modalidad.REMOTO
                : HIBRIDO.matcher(todo).find() ? Modalidad.HIBRIDO : Modalidad.PRESENCIAL;

        return Optional.of(new VacanteNueva(
                fuente,
                idExterno,
                huella(cargo, empresa, ciudad),
                cargo,
                empresa,
                ciudad,
                modalidad,
                tipo,
                linea(e.publicador(), 120),
                url,
                descripcion,
                puntos(e.responsabilidades()),
                puntos(e.requisitos()),
                carreras(carreraConsulta, cargoNormal),
                e.publicadaEn() == null || e.publicadaEn().isAfter(ahora) ? ahora : e.publicadaEn()));
    }

    /**
     * La universidad está en Medellín: una vacante presencial o híbrida solo sirve si es en el área
     * metropolitana. Las remotas sirven desde cualquier lugar.
     */
    public static boolean disponibleDesdeMedellin(VacanteNueva v) {
        return v.modalidad() == Modalidad.REMOTO || enAreaMetropolitana(v.ciudad());
    }

    private static final Pattern AREA_METROPOLITANA = Pattern.compile(
            "\\b(medellin|envigado|itagui|sabaneta|bello|la estrella|caldas|copacabana|girardota|barbosa|valle de aburra)\\b");
    /** Lo que va después de la ciudad («Caldas, Antioquia»): debe ser Antioquia o Colombia, no otro departamento. */
    private static final Pattern REGION_VALIDA = Pattern.compile("^(antioquia|ant|colombia|antioquia colombia|area metropolitana)?$");

    /**
     * «Medellín», «Envigado, Antioquia» o «Itagüí, Antioquia, Colombia» sí; «Manizales, Caldas» o
     * «Barbosa, Santander» no (Caldas y Barbosa también son nombres de otros lugares).
     */
    static boolean enAreaMetropolitana(String ciudad) {
        if (ciudad == null) return false;
        String[] partes = ciudad.split(",", 2);
        String municipio = normalizar(partes[0]);
        String region = partes.length > 1 ? normalizar(partes[1]) : "";
        return AREA_METROPOLITANA.matcher(municipio).find() && REGION_VALIDA.matcher(region).matches();
    }

    /** La carrera de la búsqueda que la encontró y las que se reconocen en el cargo. */
    static Set<String> carreras(String carreraConsulta, String cargoNormalizado) {
        Set<String> carreras = new TreeSet<>();
        if (carreraConsulta != null && ModelosVacante.CARRERAS.contains(carreraConsulta)) carreras.add(carreraConsulta);
        CARRERAS_POR_CARGO.forEach((carrera, patron) -> {
            if (patron.matcher(cargoNormalizado).find()) carreras.add(carrera);
        });
        return carreras;
    }

    /** Misma oferta en varios portales ⇒ misma huella (sin tildes, mayúsculas ni signos). */
    static String huella(String cargo, String empresa, String ciudad) {
        String base = normalizar(cargo) + "|" + normalizar(empresa) + "|" + normalizar(Objects.requireNonNullElse(ciudad, ""));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    /** Minúsculas, sin tildes y sin signos: solo letras, números y espacios simples. */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Solo enlaces http/https bien formados: nunca javascript:, data: ni similares. */
    static String urlSegura(String url) {
        if (url == null || url.isBlank() || url.length() > 2000) return null;
        try {
            URI uri = URI.create(url.trim());
            String esquema = uri.getScheme();
            if (esquema == null || uri.getHost() == null) return null;
            if (!esquema.equalsIgnoreCase("https") && !esquema.equalsIgnoreCase("http")) return null;
            return uri.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Texto de una línea, sin etiquetas HTML ni espacios repetidos, recortado. Null si queda vacío. */
    static String linea(String texto, int maximo) {
        if (texto == null) return null;
        String limpio = ETIQUETAS_HTML.matcher(texto).replaceAll(" ").replace(' ', ' ').replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() > maximo ? limpio.substring(0, maximo).trim() : limpio;
    }

    /** Texto largo: se quitan etiquetas HTML pero se conservan los saltos de línea. */
    static String texto(String texto) {
        if (texto == null) return null;
        String limpio = ETIQUETAS_HTML.matcher(texto.replaceAll("(?i)<br\\s*/?>", "\n")).replaceAll(" ")
                .replace("&nbsp;", " ").replace(' ', ' ')
                .replaceAll("[ \\t]+", " ").replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n").trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() > MAXIMO_DESCRIPCION ? limpio.substring(0, MAXIMO_DESCRIPCION).trim() : limpio;
    }

    private static List<String> puntos(List<String> puntos) {
        if (puntos == null) return List.of();
        return puntos.stream().map(p -> linea(p, 500)).filter(Objects::nonNull).limit(MAXIMO_PUNTOS).toList();
    }
}
