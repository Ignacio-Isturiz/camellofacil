import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { catchError, debounceTime, EMPTY, switchMap, tap } from 'rxjs';

import {
  Borrador,
  CampoExtraido,
  DocumentoHoja,
  PlantillaDetalle,
  ValoresHoja,
} from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { rangoMeses } from '../../../shared/fechas/fechas';
import { Icono } from '../../../shared/icono/icono';
import { AvisoError } from '../../../shared/aviso-error/aviso-error';
import { CampoExtraidoComponente } from '../componentes/campo-extraido';
import { Dialogo } from '../../../shared/dialogo/dialogo';
import { DocumentoHojaComponente } from '../../../shared/documento-hoja/documento-hoja';
import { MigasHoja } from '../componentes/migas-hoja';
import { EstadoCarga } from '../../../core/http/estado-carga';
import {
  FormularioHoja,
  crearFormularioHoja,
  educacionVacia,
  experienciaVacia,
  grupoEducacion,
  grupoExperiencia,
} from '../formulario-hoja';

const ESPERA_GUARDADO_MS = 1500;
/** Todo dato del editor es «leído»: aquí la persona escribe, no revisa. */
const CAMPO: CampoExtraido = { valor: '', estado: 'leido', ayuda: null };

type Seccion = 'contacto' | 'perfil' | 'experiencia' | 'educacion' | 'habilidades';
type EstadoGuardado = 'al-dia' | 'pendiente' | 'guardando' | 'error';

/** /hoja-de-vida/editor — editor por secciones con vista previa en vivo y guardado automático. */
@Component({
  selector: 'app-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoPipe, Icono, AvisoError, CampoExtraidoComponente, Dialogo, DocumentoHojaComponente, MigasHoja],
  templateUrl: './editor.html',
  styleUrl: './editor.scss',
})
export class Editor {
  private readonly servicio = inject(HojaDeVidaServicio);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(TranslocoService);
  private readonly destruir = inject(DestroyRef);

  protected readonly campoLeido = CAMPO;
  protected readonly secciones: readonly Seccion[] = ['contacto', 'perfil', 'experiencia', 'educacion', 'habilidades'];

  protected readonly estado = signal<EstadoCarga>('cargando');
  protected readonly borrador = signal<Borrador | null>(null);
  protected readonly plantilla = signal<PlantillaDetalle | null>(null);
  protected readonly formulario = signal<FormularioHoja | null>(null);
  private readonly valores = signal<ValoresHoja | null>(null);

  protected readonly guardado = signal<EstadoGuardado>('al-dia');
  protected readonly guardadoEn = signal<string | null>(null);
  protected readonly guardandoVersion = signal(false);
  protected readonly falloVersion = signal(false);
  private contador = 0;

  /** En pantalla estrecha solo la primera sección empieza abierta (acordeón). */
  protected readonly abiertas = signal<ReadonlySet<Seccion>>(
    new Set(this.anchoAmplio() ? this.secciones : (['contacto'] as Seccion[])),
  );

  private readonly dialogoPrevia = viewChild<Dialogo>('dialogoPrevia');

  /** El documento que se dibuja en la vista previa, armado con lo que hay en el formulario. */
  protected readonly documento = computed<DocumentoHoja | null>(() => {
    const v = this.valores();
    const b = this.borrador();
    if (!v || !b) return null;
    return this.aDocumento(v, b.correo);
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    const p = this.ruta.snapshot.queryParamMap;
    const existente = p.get('borrador');
    // Con `borrador` en la dirección se reabre ese mismo borrador; así recargar no crea otro ni pierde lo guardado.
    const origen = existente
      ? this.servicio.obtenerBorrador(existente)
      : this.servicio
          .crearBorrador({
            plantillaId: p.get('plantilla') ?? undefined,
            versionId: p.get('version') ?? undefined,
            adaptacionId: p.get('adaptacion') ?? undefined,
            idioma: (p.get('idioma') as 'es' | 'en' | null) ?? undefined,
          })
          .pipe(
            // La dirección pasa a apuntar al borrador creado (sin añadir una entrada al historial).
            tap((b) => void this.router.navigate([], { relativeTo: this.ruta, queryParams: { borrador: b.id }, replaceUrl: true })),
          );
    origen
      .pipe(
        tap((b) => this.armar(b)),
        switchMap((b) => this.servicio.plantillaDetalle(b.plantillaId)),
      )
      .subscribe({
        next: (pl) => {
          this.plantilla.set(pl);
          this.estado.set('listo');
        },
        error: () => this.estado.set('error'),
      });
  }

  private armar(b: Borrador): void {
    const f = crearFormularioHoja(b.valores);
    this.borrador.set(b);
    this.guardadoEn.set(b.guardadoEn);
    this.formulario.set(f);
    this.valores.set(f.getRawValue());

    // Cada cambio actualiza la vista previa al instante y, tras una pausa, se guarda solo.
    f.valueChanges.pipe(takeUntilDestroyed(this.destruir)).subscribe(() => {
      this.valores.set(f.getRawValue());
      this.guardado.set('pendiente');
    });
    f.valueChanges
      .pipe(
        debounceTime(ESPERA_GUARDADO_MS),
        switchMap(() => {
          if (f.invalid) return EMPTY;
          this.guardado.set('guardando');
          return this.servicio.guardarBorrador(b.id, f.getRawValue()).pipe(
            catchError(() => {
              this.guardado.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destruir),
      )
      .subscribe((guardado) => {
        this.guardado.set('al-dia');
        this.guardadoEn.set(guardado.guardadoEn);
      });
  }

  // ----- Secciones -----
  protected alternar(s: Seccion, evento: Event): void {
    const abierta = (evento.target as HTMLDetailsElement).open;
    this.abiertas.update((a) => {
      const copia = new Set(a);
      if (abierta) copia.add(s);
      else copia.delete(s);
      return copia;
    });
  }

  /** Guía de cada sección según la plantilla (se calcula una vez, no en cada revisión de la vista). */
  protected readonly guias = computed(() => {
    const guias = this.plantilla()?.guias ?? [];
    const de = (s: Seccion) => guias.find((g) => g.seccion === s) ?? null;
    return { contacto: de('contacto'), perfil: de('perfil'), experiencia: de('experiencia'), educacion: de('educacion'), habilidades: de('habilidades') };
  });

  protected agregarExperiencia(): void {
    this.formulario()?.controls.experiencia.push(
      grupoExperiencia(experienciaVacia(`nueva-${++this.contador}`)),
    );
  }

  protected quitarExperiencia(i: number): void {
    this.formulario()?.controls.experiencia.removeAt(i);
  }

  protected agregarEducacion(): void {
    this.formulario()?.controls.educacion.push(
      grupoEducacion(educacionVacia(`nueva-${++this.contador}`)),
    );
  }

  protected quitarEducacion(i: number): void {
    this.formulario()?.controls.educacion.removeAt(i);
  }

  protected readonly previaAbierta = signal(false);

  protected verPrevia(): void {
    this.previaAbierta.set(true);
    this.dialogoPrevia()?.abrir();
  }

  /** Guarda lo escrito y lo convierte en una versión de la hoja. */
  protected guardarComoVersion(): void {
    const b = this.borrador();
    const f = this.formulario();
    if (!b || !f || f.invalid || this.guardandoVersion()) return;
    this.guardandoVersion.set(true);
    this.falloVersion.set(false);
    this.servicio
      .guardarBorrador(b.id, f.getRawValue())
      .pipe(switchMap(() => this.servicio.guardarBorradorComoVersion(b.id)))
      .subscribe({
        next: () => {
          this.servicio.cargarResumen(true);
          this.servicio.cargarVersiones();
          void this.router.navigate(['/hoja-de-vida/versiones']);
        },
        error: () => {
          this.guardandoVersion.set(false);
          this.falloVersion.set(true);
        },
      });
  }

  // ----- Utilidades -----
  private anchoAmplio(): boolean {
    try {
      return window.matchMedia('(min-width: 900px)').matches;
    } catch {
      return true;
    }
  }

  /** Arma el documento de la vista previa solo con lo que la persona escribió (no agrega nada). */
  private aDocumento(v: ValoresHoja, correo: string): DocumentoHoja {
    const t = (clave: string) => this.i18n.translate(`hoja.revisar.secciones.${clave}`);
    const secciones: DocumentoHoja['secciones'][number][] = [];

    if (v.perfil.trim()) secciones.push({ id: 'perfil', titulo: t('perfil'), bloques: [v.perfil.trim()] });

    const exp = v.experiencia
      .filter((e) => e.cargo || e.organizacion || e.descripcion)
      .map((e) => {
        const cabecera = [e.cargo, e.organizacion].filter(Boolean).join(', ');
        const fechas = rangoMeses(e.inicio, e.fin, this.i18n.getActiveLang());
        return [fechas ? `${cabecera} · ${fechas}` : cabecera, e.descripcion].filter(Boolean).join(' — ');
      });
    if (exp.length) secciones.push({ id: 'experiencia', titulo: t('experiencia'), bloques: exp });

    const edu = v.educacion
      .filter((e) => e.titulo || e.institucion)
      .map((e) => {
        const base = [e.institucion, e.titulo].filter(Boolean).join(' · ');
        const fechas = rangoMeses(e.inicio, e.fin, this.i18n.getActiveLang());
        return fechas ? `${base} · ${fechas}` : base;
      });
    if (edu.length) secciones.push({ id: 'educacion', titulo: t('educacion'), bloques: edu });

    if (v.habilidades.trim()) secciones.push({ id: 'habilidades', titulo: t('habilidades'), bloques: [v.habilidades.trim()] });

    return {
      nombre: v.nombre,
      contacto: [correo, v.telefono, v.ciudad].filter(Boolean),
      secciones,
    };
  }
}
