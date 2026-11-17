import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';

import { DatosExtraidos, ValoresHoja } from '../../core/hoja-de-vida/hoja-de-vida.modelos';

/**
 * Formulario tipado de la hoja de vida, compartido por «Revisa lo que leímos» y el editor.
 * Los límites de largo son los mismos que valida el backend.
 */
const texto = (valor: string, max = 200) =>
  new FormControl(valor, { nonNullable: true, validators: [Validators.maxLength(max)] });

export type ExperienciaHoja = ValoresHoja['experiencia'][number];
export type EducacionHoja = ValoresHoja['educacion'][number];

export const grupoExperiencia = (e: ExperienciaHoja) =>
  new FormGroup({
    id: new FormControl(e.id, { nonNullable: true }),
    cargo: texto(e.cargo),
    organizacion: texto(e.organizacion),
    inicio: texto(e.inicio, 7),
    fin: texto(e.fin, 7),
    descripcion: texto(e.descripcion, 1000),
  });

export const grupoEducacion = (e: EducacionHoja) =>
  new FormGroup({
    id: new FormControl(e.id, { nonNullable: true }),
    titulo: texto(e.titulo),
    institucion: texto(e.institucion),
    inicio: texto(e.inicio, 7),
    fin: texto(e.fin, 7),
  });

export const crearFormularioHoja = (v: ValoresHoja) =>
  new FormGroup({
    nombre: texto(v.nombre),
    telefono: texto(v.telefono, 30),
    ciudad: texto(v.ciudad, 100),
    perfil: texto(v.perfil, 1500),
    habilidades: texto(v.habilidades, 1000),
    experiencia: new FormArray(v.experiencia.map(grupoExperiencia)),
    educacion: new FormArray(v.educacion.map(grupoEducacion)),
  });

export type FormularioHoja = ReturnType<typeof crearFormularioHoja>;

export const experienciaVacia = (id: string): ExperienciaHoja => ({
  id,
  cargo: '',
  organizacion: '',
  inicio: '',
  fin: '',
  descripcion: '',
});

export const educacionVacia = (id: string): EducacionHoja => ({ id, titulo: '', institucion: '', inicio: '', fin: '' });

/** Solo los valores de lo que se leyó del archivo (sin el estado de lectura de cada dato). */
export function valoresLeidos(d: DatosExtraidos): ValoresHoja {
  return {
    nombre: d.contacto.nombre.valor,
    telefono: d.contacto.telefono.valor,
    ciudad: d.contacto.ciudad.valor,
    perfil: d.perfil.valor,
    habilidades: d.habilidades.valor,
    experiencia: d.experiencia.map((e) => ({
      id: e.id,
      cargo: e.cargo.valor,
      organizacion: e.organizacion.valor,
      inicio: e.inicio.valor,
      fin: e.fin.valor,
      descripcion: e.descripcion.valor,
    })),
    educacion: d.educacion.map((e) => ({
      id: e.id,
      titulo: e.titulo.valor,
      institucion: e.institucion.valor,
      inicio: e.inicio.valor,
      fin: e.fin.valor,
    })),
  };
}
