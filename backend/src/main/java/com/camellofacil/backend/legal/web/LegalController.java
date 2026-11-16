package com.camellofacil.backend.legal.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalController {

    private final SesionActual sesion;
    private final ConsentimientoService consentimientos;

    public LegalController(SesionActual sesion, ConsentimientoService consentimientos) {
        this.sesion = sesion;
        this.consentimientos = consentimientos;
    }

    public record AutorizacionSolicitud(@NotNull Boolean activa) {}

    /** Activa o revoca la autorización para analizar la hoja de vida con IA. */
    @PutMapping("/analisis-ia")
    public ResponseEntity<Void> analisisIa(@Valid @RequestBody AutorizacionSolicitud solicitud) {
        UUID usuarioId = sesion.requerido().id();
        if (solicitud.activa()) {
            consentimientos.otorgar(usuarioId, TipoConsentimiento.ANALISIS_IA);
        } else {
            consentimientos.revocar(usuarioId, TipoConsentimiento.ANALISIS_IA);
        }
        return ResponseEntity.noContent().build();
    }
}
