package com.camellofacil.backend.legal.app;

import com.camellofacil.backend.legal.persistence.Consentimiento;
import com.camellofacil.backend.legal.persistence.ConsentimientoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentimientoService {

    /**
     * Versión vigente de los términos y la política de privacidad (borrador 1.0, pendiente de revisión legal).
     * Al publicar una versión nueva se sube este valor: quien aceptó una anterior debe aceptar de nuevo.
     */
    public static final String VERSION_TERMINOS = "1.0";
    /** Versión del texto de autorización para el análisis con IA. */
    public static final String VERSION_ANALISIS_IA = "1.0";

    private final ConsentimientoRepository repositorio;

    public ConsentimientoService(ConsentimientoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public static String versionVigente(TipoConsentimiento tipo) {
        return tipo == TipoConsentimiento.TERMINOS_PRIVACIDAD ? VERSION_TERMINOS : VERSION_ANALISIS_IA;
    }

    /** Registra el consentimiento en la versión vigente. Si ya existe uno activo en esa versión, no duplica. */
    @Transactional
    public ConsentimientoVigente otorgar(UUID usuarioId, TipoConsentimiento tipo) {
        String version = versionVigente(tipo);
        Optional<Consentimiento> actual = activo(usuarioId, tipo);
        if (actual.isPresent() && actual.get().getVersion().equals(version)) {
            return aVigente(actual.get());
        }
        actual.ifPresent(anterior -> anterior.revocar(Instant.now()));
        return aVigente(repositorio.save(new Consentimiento(usuarioId, tipo.name(), version, Instant.now())));
    }

    @Transactional
    public void revocar(UUID usuarioId, TipoConsentimiento tipo) {
        activo(usuarioId, tipo).ifPresent(c -> c.revocar(Instant.now()));
    }

    /** El consentimiento activo, solo si es de la versión vigente. */
    @Transactional(readOnly = true)
    public Optional<ConsentimientoVigente> vigente(UUID usuarioId, TipoConsentimiento tipo) {
        return activo(usuarioId, tipo)
                .filter(c -> c.getVersion().equals(versionVigente(tipo)))
                .map(ConsentimientoService::aVigente);
    }

    private Optional<Consentimiento> activo(UUID usuarioId, TipoConsentimiento tipo) {
        return repositorio.findFirstByUsuarioIdAndTipoAndRevocadoEnIsNullOrderByOtorgadoEnDesc(usuarioId, tipo.name());
    }

    private static ConsentimientoVigente aVigente(Consentimiento c) {
        return new ConsentimientoVigente(c.getVersion(), c.getOtorgadoEn());
    }
}
