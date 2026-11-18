package com.camellofacil.backend.plantillas.web;

import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Archivo;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Formato;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Respuesta de descarga de una hoja de vida, igual para versiones y adaptaciones. */
public final class Descargas {

    private Descargas() {}

    /** «pdf» o «docx»; otro valor es un 400 con un mensaje claro. */
    public static Formato formato(String texto) {
        return Formato.de(texto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no válido: usa pdf o docx"));
    }

    /** El nombre es genérico a propósito: no lleva el nombre de la persona. Y nunca se guarda en caché. */
    public static ResponseEntity<byte[]> respuesta(Archivo archivo, String nombreBase) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.formato().tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreBase + "." + archivo.formato().extension()).build().toString())
                .cacheControl(CacheControl.noStore())
                .contentLength(archivo.contenido().length)
                .body(archivo.contenido());
    }
}
