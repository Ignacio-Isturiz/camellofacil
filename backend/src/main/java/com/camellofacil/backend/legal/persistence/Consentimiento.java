package com.camellofacil.backend.legal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Un consentimiento otorgado por una persona. Al revocarlo se conserva el registro con la fecha de revocación. */
@Entity
@Table(name = "consentimiento")
public class Consentimiento {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 10)
    private String version;

    @Column(name = "otorgado_en", nullable = false)
    private Instant otorgadoEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    protected Consentimiento() {
        // Requerido por JPA
    }

    public Consentimiento(UUID usuarioId, String tipo, String version, Instant otorgadoEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.version = version;
        this.otorgadoEn = otorgadoEn;
    }

    public void revocar(Instant ahora) {
        this.revocadoEn = ahora;
    }

    public String getVersion() {
        return version;
    }

    public Instant getOtorgadoEn() {
        return otorgadoEn;
    }
}
