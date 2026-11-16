import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Carrera, SEMESTRES, TipoPersona } from '../../../core/perfil/perfil.modelos';
import { PerfilServicio } from '../../../core/perfil/perfil.servicio';

/**
 * Modal obligatorio del primer ingreso (RF-04 a RF-07): no tiene botón de cerrar, Esc no lo cierra y el clic en
 * el fondo no hace nada. La única salida, además de completarlo, es cerrar sesión.
 */
@Component({
  selector: 'app-completar-perfil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe],
  templateUrl: './completar-perfil.html',
  styleUrl: './completar-perfil.scss',
})
export class CompletarPerfil {
  private readonly perfil = inject(PerfilServicio);

  /** Pide al marco cerrar la sesión. */
  readonly salir = output<void>();

  private readonly caja = viewChild.required<ElementRef<HTMLDialogElement>>('caja');

  protected readonly nombre = computed(() => this.perfil.estado()?.nombre.split(' ')[0] ?? '');
  protected readonly version = computed(() => this.perfil.estado()?.versionTerminosVigente ?? '');
  protected readonly semestres = SEMESTRES;
  protected readonly carreras = signal<Carrera[]>([]);
  protected readonly errorCarreras = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorEnvio = signal(false);
  protected readonly intentado = signal(false);

  protected readonly formulario = new FormGroup({
    tipo: new FormControl<TipoPersona | null>(null, Validators.required),
    carrera: new FormControl('', { nonNullable: true, validators: Validators.required }),
    semestre: new FormControl<number | null>(null),
    aceptaTerminos: new FormControl(false, { nonNullable: true, validators: Validators.requiredTrue }),
    autorizaAnalisisIa: new FormControl(false, { nonNullable: true, validators: Validators.requiredTrue }),
  });

  private readonly tipo = toSignal(this.formulario.controls.tipo.valueChanges, { initialValue: null });
  protected readonly esEstudiante = computed(() => this.tipo() === 'ESTUDIANTE');

  constructor() {
    // El semestre solo es obligatorio para estudiantes; a egresados no se les pregunta.
    this.formulario.controls.tipo.valueChanges.pipe(takeUntilDestroyed()).subscribe((tipo) => {
      const semestre = this.formulario.controls.semestre;
      semestre.setValidators(tipo === 'ESTUDIANTE' ? Validators.required : null);
      if (tipo !== 'ESTUDIANTE') semestre.setValue(null);
      semestre.updateValueAndValidity();
    });
    afterNextRender(() => this.caja().nativeElement.showModal());
    void this.cargarCarreras();
  }

  protected async cargarCarreras(): Promise<void> {
    this.errorCarreras.set(false);
    try {
      this.carreras.set(await this.perfil.carreras());
    } catch {
      this.errorCarreras.set(true);
    }
  }

  /** Esc dispara «cancel» en el diálogo nativo: se ignora para que el modal no se pueda saltar. */
  protected impedirCierre(evento: Event): void {
    evento.preventDefault();
  }

  protected error(campo: keyof typeof this.formulario.controls): boolean {
    return this.intentado() && this.formulario.controls[campo].invalid;
  }

  protected async enviar(): Promise<void> {
    this.intentado.set(true);
    if (this.formulario.invalid || this.enviando()) return;
    const v = this.formulario.getRawValue();
    this.enviando.set(true);
    this.errorEnvio.set(false);
    try {
      await this.perfil.completar({
        tipo: v.tipo!,
        carrera: v.carrera,
        semestre: v.tipo === 'ESTUDIANTE' ? v.semestre : null,
        versionTerminos: this.version(),
        aceptaTerminos: v.aceptaTerminos,
        autorizaAnalisisIa: v.autorizaAnalisisIa,
      });
      this.caja().nativeElement.close();
    } catch {
      this.errorEnvio.set(true);
    } finally {
      this.enviando.set(false);
    }
  }
}
