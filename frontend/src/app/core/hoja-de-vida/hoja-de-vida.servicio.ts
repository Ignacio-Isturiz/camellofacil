import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subscription, switchMap } from 'rxjs';

import { API_HOJA_DE_VIDA } from './hoja-de-vida.api';
import {
  Borrador,
  CambioPropuesto,
  DatosExtraidos,
  DocumentoHoja,
  Plantilla,
  PlantillaDetalle,
  PropuestaReorganizacion,
  PuntajeAts,
  ResumenHojaDeVida,
  SolicitudBorrador,
  SolicitudReorganizacion,
  ValoresHoja,
  Version,
} from './hoja-de-vida.modelos';
import { EstadoCarga } from '../http/estado-carga';

/** Si la primera respuesta tarda más que esto, se avisa «Preparando tu espacio…» (el servidor gratuito despierta lento). */
const ESPERA_ARRANQUE_MS = 4000;

/**
 * Datos del módulo «Hoja de vida». Todo viene del API real con HttpClient; mientras no exista,
 * cada pantalla muestra su estado de error (con «Reintentar») y no se rompe.
 * No se guarda nada de la hoja en localStorage ni sessionStorage (Ley 1581).
 */
@Injectable({ providedIn: 'root' })
export class HojaDeVidaServicio {
  private readonly http = inject(HttpClient);

  readonly #resumen = signal<ResumenHojaDeVida | null>(null);
  readonly resumen = this.#resumen.asReadonly();
  readonly #estadoResumen = signal<EstadoCarga>('cargando');
  readonly estadoResumen = this.#estadoResumen.asReadonly();
  /** Verdadero cuando la primera respuesta se está demorando. */
  readonly #despertando = signal(false);
  readonly despertando = this.#despertando.asReadonly();

  readonly #plantillas = signal<readonly Plantilla[]>([]);
  readonly plantillas = this.#plantillas.asReadonly();
  readonly #estadoPlantillas = signal<EstadoCarga>('cargando');
  readonly estadoPlantillas = this.#estadoPlantillas.asReadonly();

  readonly #versiones = signal<readonly Version[]>([]);
  readonly versiones = this.#versiones.asReadonly();
  readonly #estadoVersiones = signal<EstadoCarga>('cargando');
  readonly estadoVersiones = this.#estadoVersiones.asReadonly();

  readonly #puntajeAts = signal<PuntajeAts | null>(null);
  readonly puntajeAts = this.#puntajeAts.asReadonly();
  readonly #estadoPuntajeAts = signal<EstadoCarga>('listo');
  readonly estadoPuntajeAts = this.#estadoPuntajeAts.asReadonly();

  /** Recomendaciones de la IA: se piden aparte porque tardan unos segundos. `limite`: ya usó las de hoy. */
  readonly #estadoIa = signal<'inactivo' | 'generando' | 'error' | 'limite'>('inactivo');
  readonly estadoIa = this.#estadoIa.asReadonly();

  private resumenEnCurso: Subscription | null = null;
  private puntajeEnCurso: Subscription | null = null;

  /**
   * Varias pantallas lo piden al entrar (Mi perfil, Hoja de vida): si ya hay una petición en vuelo, se reutiliza.
   * `forzar` (tras guardar algo que lo cambia) cancela la que esté en vuelo y pide de nuevo.
   */
  cargarResumen(forzar = false): void {
    if (!forzar && this.resumenEnCurso && !this.resumenEnCurso.closed) return;
    this.resumenEnCurso?.unsubscribe();
    this.#estadoResumen.set('cargando');
    const aviso = setTimeout(() => this.#despertando.set(true), ESPERA_ARRANQUE_MS);
    this.resumenEnCurso = this.http.get<ResumenHojaDeVida>(API_HOJA_DE_VIDA.resumen).subscribe({
      next: (r) => {
        clearTimeout(aviso);
        this.#despertando.set(false);
        this.#resumen.set(r);
        this.#puntajeAts.set(r.hoja?.ats ?? null);
        if (this.#estadoIa() !== 'generando') this.#estadoIa.set('inactivo');
        this.#estadoResumen.set('listo');
      },
      error: () => {
        clearTimeout(aviso);
        this.#despertando.set(false);
        this.#estadoResumen.set('error');
      },
    });
  }

  readonly #datos = signal<DatosExtraidos | null>(null);
  readonly datos = this.#datos.asReadonly();
  readonly #estadoDatos = signal<EstadoCarga>('cargando');
  readonly estadoDatos = this.#estadoDatos.asReadonly();

  cargarDatos(): void {
    this.#estadoDatos.set('cargando');
    this.http.get<DatosExtraidos>(API_HOJA_DE_VIDA.datos).subscribe({
      next: (d) => {
        this.#datos.set(d);
        this.#estadoDatos.set('listo');
      },
      error: () => this.#estadoDatos.set('error'),
    });
  }

  /** Guarda lo corregido y confirma: el backend analiza Harvard / ATS con exactamente esa información. */
  guardarYConfirmar(valores: ValoresHoja): Observable<unknown> {
    return this.http
      .put<void>(API_HOJA_DE_VIDA.datos, valores)
      .pipe(switchMap(() => this.http.post<void>(API_HOJA_DE_VIDA.confirmarDatos, {})));
  }

  /** Las plantillas casi no cambian: se piden una vez por visita (o al reintentar tras un error). */
  cargarPlantillas(): void {
    if (this.estadoPlantillas() === 'listo') return;
    this.#estadoPlantillas.set('cargando');
    this.http.get<readonly Plantilla[]>(API_HOJA_DE_VIDA.plantillas).subscribe({
      next: (lista) => {
        this.#plantillas.set(lista);
        this.#estadoPlantillas.set('listo');
      },
      error: () => this.#estadoPlantillas.set('error'),
    });
  }

  cargarVersiones(): void {
    this.#estadoVersiones.set('cargando');
    this.http.get<readonly Version[]>(API_HOJA_DE_VIDA.versiones).subscribe({
      next: (lista) => {
        this.#versiones.set(lista);
        this.#estadoVersiones.set('listo');
      },
      error: () => this.#estadoVersiones.set('error'),
    });
  }

  /**
   * Pide las recomendaciones de la IA. Trae el resumen ya con ellas; el puntaje ATS que la persona eligió no se toca.
   * Si la hoja no cambió desde la última vez, el servidor devuelve las guardadas sin volver a llamar a la IA.
   */
  generarRecomendacionesIa(): void {
    if (this.#estadoIa() === 'generando') return;
    this.#estadoIa.set('generando');
    this.http.post<ResumenHojaDeVida>(API_HOJA_DE_VIDA.recomendacionesIa, {}).subscribe({
      next: (r) => {
        this.#resumen.set(r);
        this.#estadoIa.set('inactivo');
      },
      error: (e: HttpErrorResponse) => this.#estadoIa.set(e.status === 429 ? 'limite' : 'error'),
    });
  }

  /** Recalcula el puntaje ATS con otra opción de evaluación. */
  evaluarAts(opcionId: string): void {
    this.#estadoPuntajeAts.set('cargando');
    this.puntajeEnCurso?.unsubscribe();
    this.puntajeEnCurso = this.http.get<PuntajeAts>(API_HOJA_DE_VIDA.puntajeAts(opcionId)).subscribe({
      next: (p) => {
        this.#puntajeAts.set(p);
        this.#estadoPuntajeAts.set('listo');
      },
      error: () => this.#estadoPuntajeAts.set('error'),
    });
  }

  // ---------- Llamadas puntuales (el estado de pantalla lo lleva cada componente) ----------

  plantillaDetalle(id: string): Observable<PlantillaDetalle> {
    return this.http.get<PlantillaDetalle>(API_HOJA_DE_VIDA.plantilla(id));
  }

  vistaPreviaPlantilla(id: string, idioma: string): Observable<DocumentoHoja> {
    return this.http.get<DocumentoHoja>(API_HOJA_DE_VIDA.vistaPreviaPlantilla(id, idioma));
  }

  documentoVersion(id: string): Observable<DocumentoHoja> {
    return this.http.get<DocumentoHoja>(API_HOJA_DE_VIDA.documentoVersion(id));
  }

  renombrarVersion(id: string, nombre: string): Observable<Version> {
    return this.http.patch<Version>(API_HOJA_DE_VIDA.version(id), { nombre });
  }

  duplicarVersion(id: string): Observable<Version> {
    return this.http.post<Version>(API_HOJA_DE_VIDA.duplicarVersion(id), {});
  }

  usarVersionComoBase(id: string): Observable<unknown> {
    return this.http.post(API_HOJA_DE_VIDA.usarVersionComoBase(id), {});
  }

  eliminarVersion(id: string): Observable<unknown> {
    return this.http.delete(API_HOJA_DE_VIDA.version(id));
  }

  proponerReorganizacion(solicitud: SolicitudReorganizacion): Observable<PropuestaReorganizacion> {
    return this.http.post<PropuestaReorganizacion>(API_HOJA_DE_VIDA.reorganizaciones, solicitud);
  }

  /** Aplica o revierte un cambio; el backend devuelve la propuesta recalculada. */
  cambiarCambio(propuestaId: string, cambio: CambioPropuesto, aplicado: boolean): Observable<PropuestaReorganizacion> {
    return this.http.put<PropuestaReorganizacion>(API_HOJA_DE_VIDA.cambioReorganizacion(propuestaId, cambio.id), {
      aplicado,
    });
  }

  aceptarReorganizacion(propuestaId: string): Observable<Version> {
    return this.http.post<Version>(API_HOJA_DE_VIDA.aceptarReorganizacion(propuestaId), {});
  }

  descartarReorganizacion(propuestaId: string): Observable<unknown> {
    return this.http.delete(API_HOJA_DE_VIDA.reorganizacion(propuestaId));
  }

  crearBorrador(solicitud: SolicitudBorrador): Observable<Borrador> {
    return this.http.post<Borrador>(API_HOJA_DE_VIDA.borradores, solicitud);
  }

  /** Vuelve a abrir un borrador ya creado (al recargar el editor). */
  obtenerBorrador(id: string): Observable<Borrador> {
    return this.http.get<Borrador>(API_HOJA_DE_VIDA.borrador(id));
  }

  guardarBorrador(id: string, valores: ValoresHoja): Observable<Borrador> {
    return this.http.put<Borrador>(API_HOJA_DE_VIDA.borrador(id), valores);
  }

  guardarBorradorComoVersion(id: string): Observable<Version> {
    return this.http.post<Version>(API_HOJA_DE_VIDA.guardarBorradorComoVersion(id), {});
  }
}
