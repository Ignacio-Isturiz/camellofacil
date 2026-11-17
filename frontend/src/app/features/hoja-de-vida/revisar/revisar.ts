import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import {
  CampoExtraido,
  DatosExtraidos,
  EducacionExtraida,
  ExperienciaExtraida,
  ValoresHoja,
} from '../../../core/hoja-de-vida/hoja-de-vida.modelos';
import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { Icono } from '../../../shared/icono/icono';
import { AvisoError } from '../../../shared/aviso-error/aviso-error';
import { CampoExtraidoComponente } from '../componentes/campo-extraido';
import { MigasHoja } from '../componentes/migas-hoja';
import { VistaPreviaHoja } from '../componentes/vista-previa-hoja';
import { FormularioHoja, crearFormularioHoja, experienciaVacia, grupoExperiencia, valoresLeidos } from '../formulario-hoja';

const CAMPO_VACIO: CampoExtraido = { valor: '', estado: 'leido', ayuda: null };

/** /hoja-de-vida/revisar — confirmar (y corregir) los datos que se leyeron del archivo. */
@Component({
  selector: 'app-revisar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslocoPipe,
    Icono,
    AvisoError,
    CampoExtraidoComponente,
    MigasHoja,
    VistaPreviaHoja,
  ],
  templateUrl: './revisar.html',
  styleUrl: './revisar.scss',
})
export class Revisar {
  private readonly servicio = inject(HojaDeVidaServicio);
  private readonly router = inject(Router);
  private readonly destruir = inject(DestroyRef);

  protected readonly estado = this.servicio.estadoDatos;
  protected readonly datos = this.servicio.datos;

  protected readonly formulario = signal<FormularioHoja | null>(null);
  /** Metadatos de lectura de cada elemento de las listas (en el mismo orden que el formulario). */
  protected readonly metaExperiencia = signal<readonly ExperienciaExtraida[]>([]);
  protected readonly metaEducacion = signal<readonly EducacionExtraida[]>([]);
  private contadorNuevos = 0;

  /** Claves de los datos que la persona ya tocó (dejan de estar pendientes). */
  private readonly resueltos = signal<ReadonlySet<string>>(new Set());
  protected readonly valores = signal<ValoresHoja | null>(null);

  /** Datos marcados «revisa» que aún no se han tocado. */
  protected readonly marcas = computed<ReadonlySet<string>>(() => {
    const d = this.datos();
    if (!d) return new Set();
    const hechos = this.resueltos();
    const claves: string[] = [];
    const evaluar = (clave: string, c: CampoExtraido) => {
      if (c.estado === 'revisa' && !hechos.has(clave)) claves.push(clave);
    };
    evaluar('nombre', d.contacto.nombre);
    evaluar('telefono', d.contacto.telefono);
    evaluar('ciudad', d.contacto.ciudad);
    evaluar('perfil', d.perfil);
    evaluar('habilidades', d.habilidades);
    for (const e of this.metaExperiencia()) {
      evaluar(`exp.${e.id}.cargo`, e.cargo);
      evaluar(`exp.${e.id}.organizacion`, e.organizacion);
      evaluar(`exp.${e.id}.inicio`, e.inicio);
      evaluar(`exp.${e.id}.fin`, e.fin);
      evaluar(`exp.${e.id}.descripcion`, e.descripcion);
    }
    for (const e of this.metaEducacion()) {
      evaluar(`edu.${e.id}.titulo`, e.titulo);
      evaluar(`edu.${e.id}.institucion`, e.institucion);
      evaluar(`edu.${e.id}.inicio`, e.inicio);
      evaluar(`edu.${e.id}.fin`, e.fin);
    }
    return new Set(claves);
  });
  protected readonly pendientes = computed(() => this.marcas().size);

  private contar(coincide: (clave: string) => boolean): number {
    return [...this.marcas()].filter(coincide).length;
  }
  protected readonly pendientesContacto = computed(() => this.contar((c) => ['nombre', 'telefono', 'ciudad'].includes(c)));
  protected readonly pendientesPerfil = computed(() => this.contar((c) => c === 'perfil'));
  protected readonly pendientesExperiencia = computed(() => this.contar((c) => c.startsWith('exp.')));
  protected readonly pendientesEducacion = computed(() => this.contar((c) => c.startsWith('edu.')));
  protected readonly pendientesHabilidades = computed(() => this.contar((c) => c === 'habilidades'));

  /** El correo viene de la cuenta: se muestra, pero no se edita ni se envía. */
  protected readonly correoSoloLectura = new FormControl('', { nonNullable: true });

  protected readonly enviando = signal(false);
  protected readonly errorGuardar = signal(false);
  protected readonly vistaAbierta = signal(false);

  constructor() {
    this.servicio.cargarDatos();
    // Cuando llegan los datos se arma el formulario (una sola vez por carga).
    effect(() => {
      const d = this.datos();
      if (d && this.estado() === 'listo') this.armar(d);
    });
  }

  protected reintentar(): void {
    this.servicio.cargarDatos();
  }

  private armar(d: DatosExtraidos): void {
    const f = crearFormularioHoja(valoresLeidos(d));
    this.resueltos.set(new Set());
    this.correoSoloLectura.setValue(d.contacto.correo.valor);
    this.metaExperiencia.set(d.experiencia);
    this.metaEducacion.set(d.educacion);
    this.formulario.set(f);
    this.valores.set(f.getRawValue());
    f.valueChanges.pipe(takeUntilDestroyed(this.destruir)).subscribe(() => this.valores.set(f.getRawValue()));
  }

  protected resolver(clave: string): void {
    this.resueltos.update((s) => new Set(s).add(clave));
  }

  protected agregarExperiencia(): void {
    const f = this.formulario();
    if (!f) return;
    const id = `nueva-${++this.contadorNuevos}`;
    const nueva: ExperienciaExtraida = {
      id,
      cargo: CAMPO_VACIO,
      organizacion: CAMPO_VACIO,
      inicio: CAMPO_VACIO,
      fin: CAMPO_VACIO,
      descripcion: CAMPO_VACIO,
    };
    f.controls.experiencia.push(grupoExperiencia(experienciaVacia(id)));
    this.metaExperiencia.update((l) => [...l, nueva]);
  }

  protected quitarExperiencia(indice: number): void {
    const f = this.formulario();
    if (!f) return;
    f.controls.experiencia.removeAt(indice);
    this.metaExperiencia.update((l) => l.filter((_, i) => i !== indice));
  }

  protected confirmar(): void {
    const f = this.formulario();
    if (!f || f.invalid || this.enviando()) return;
    this.enviando.set(true);
    this.errorGuardar.set(false);
    this.servicio.guardarYConfirmar(f.getRawValue()).subscribe({
      next: () => {
        this.servicio.cargarResumen(true);
        void this.router.navigate(['/hoja-de-vida']);
      },
      error: () => {
        this.enviando.set(false);
        this.errorGuardar.set(true);
      },
    });
  }
}
