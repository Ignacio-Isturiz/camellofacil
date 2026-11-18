package com.camellofacil.backend.plantillas.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.Plantilla;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.PlantillaDetalle;
import com.camellofacil.backend.plantillas.app.PlantillasService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plantillas")
public class PlantillasController {

    private final SesionActual sesion;
    private final PlantillasService plantillas;

    public PlantillasController(SesionActual sesion, PlantillasService plantillas) {
        this.sesion = sesion;
        this.plantillas = plantillas;
    }

    @GetMapping
    public List<Plantilla> listar() {
        sesion.requerido();
        return plantillas.listar();
    }

    @GetMapping("/{id}")
    public PlantillaDetalle detalle(@PathVariable String id) {
        sesion.requerido();
        return plantillas.detalle(id);
    }

    @GetMapping("/{id}/vista-previa")
    public DocumentoHoja vistaPrevia(@PathVariable String id, @RequestParam(defaultValue = "es") String idioma) {
        UsuarioActual usuario = sesion.requerido();
        return plantillas.vistaPrevia(usuario.id(), usuario.correo(), id, idioma);
    }
}
