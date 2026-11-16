package com.camellofacil.backend.cv.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Hoja de vida base de una persona. Los valores de dominio (estado, tipo, datos) los maneja la capa app. */
@Entity
@Table(name = "hoja_de_vida")
public class HojaDeVida {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 4)
    private String tipoArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private int tamanoBytes;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(length = 2)
    private String idioma;

    private Short paginas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String datos;

    @Column(nullable = false)
    private short intentos;

    @Column(name = "siguiente_intento_en")
    private Instant siguienteIntentoEn;

    @Column(name = "subida_en", nullable = false)
    private Instant subidaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    protected HojaDeVida() {
        // Requerido por JPA
    }

    public HojaDeVida(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** Un archivo nuevo reemplaza al anterior y vuelve a la cola de análisis. */
    public void nuevoArchivo(String nombreArchivo, String tipoArchivo, int tamanoBytes, String estadoInicial, Instant ahora) {
        this.nombreArchivo = nombreArchivo;
        this.tipoArchivo = tipoArchivo;
        this.tamanoBytes = tamanoBytes;
        this.estado = estadoInicial;
        this.idioma = null;
        this.paginas = null;
        this.datos = null;
        this.intentos = 0;
        this.siguienteIntentoEn = null;
        this.subidaEn = ahora;
        this.actualizadaEn = ahora;
    }

    /** Reserva el análisis por un tiempo para que otra copia del servidor no lo tome a la vez. */
    public void reservar(Instant hasta) {
        this.intentos++;
        this.siguienteIntentoEn = hasta;
    }

    public void reintentarEn(Instant cuando) {
        this.siguienteIntentoEn = cuando;
    }

    public void cambiarEstado(String estado, Instant ahora) {
        this.estado = estado;
        this.actualizadaEn = ahora;
    }

    public void resultado(String estado, String idioma, Short paginas, String datos, Instant ahora) {
        this.estado = estado;
        this.idioma = idioma;
        this.paginas = paginas;
        this.datos = datos;
        this.siguienteIntentoEn = null;
        this.actualizadaEn = ahora;
    }

    public void corregirDatos(String datos, Instant ahora) {
        this.datos = datos;
        this.actualizadaEn = ahora;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public String getEstado() {
        return estado;
    }

    public String getIdioma() {
        return idioma;
    }

    public Short getPaginas() {
        return paginas;
    }

    public String getDatos() {
        return datos;
    }

    public short getIntentos() {
        return intentos;
    }

    public Instant getSubidaEn() {
        return subidaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }
}
