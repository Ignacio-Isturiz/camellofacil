package com.camellofacil.backend.plantillas.app;

import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.Plantilla;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.PlantillaDetalle;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Consulta de plantillas y armado de documentos. Servicio público del módulo. */
@Service
public class PlantillasService {

    private final CatalogoPlantillas catalogo;
    private final RenderizadorDocumento renderizador;
    private final HojaDeVidaService hojas;

    public PlantillasService(CatalogoPlantillas catalogo, RenderizadorDocumento renderizador, HojaDeVidaService hojas) {
        this.catalogo = catalogo;
        this.renderizador = renderizador;
        this.hojas = hojas;
    }

    public List<Plantilla> listar() {
        return catalogo.listar();
    }

    public PlantillaDetalle detalle(String id) {
        return catalogo.buscar(id).orElseThrow(() -> new PlantillaException("Esa plantilla no existe"));
    }

    /** La plantilla con los datos confirmados de la persona (exige la hoja base). */
    public DocumentoHoja vistaPrevia(UUID usuarioId, String correo, String plantillaId, String idioma) {
        return armar(plantillaId, hojas.valores(usuarioId), correo, idioma);
    }

    /** Arma el documento de unos valores ya revisados (editor y versiones). */
    public DocumentoHoja armar(String plantillaId, ValoresHoja valores, String correo, String idioma) {
        PlantillaDetalle plantilla = detalle(plantillaId);
        return renderizador.renderizar(plantilla, valores, correo, idiomaValido(plantilla, idioma));
    }

    private static String idiomaValido(PlantillaDetalle plantilla, String idioma) {
        String elegido = idioma == null || idioma.isBlank() ? "es" : idioma;
        if (!plantilla.idiomas().contains(elegido)) {
            throw new PlantillaException("Esa plantilla no está disponible en ese idioma");
        }
        return elegido;
    }
}
