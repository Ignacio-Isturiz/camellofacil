package com.camellofacil.backend.plantillas.app;

/** La plantilla pedida no existe o no está disponible en ese idioma. */
public class PlantillaException extends RuntimeException {

    public PlantillaException(String mensaje) {
        super(mensaje);
    }
}
