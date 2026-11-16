import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import {
  Carrera,
  Disponibilidad,
  DISPONIBILIDADES,
  ModalidadPerfil,
  MODALIDADES_PERFIL,
  SEMESTRES,
  TipoPersona,
} from '../../../core/perfil/perfil.modelos';
import { PerfilServicio } from '../../../core/perfil/perfil.servicio';
import { Icono } from '../../../shared/icono/icono';
import { ToastServicio } from '../../../shared/toast/toast.servicio';

/** /perfil/editar — datos personales (RF-05 a RF-07) y preferencias de búsqueda (modalidad y disponibilidad, opcionales). */
@Component({
  selector: 'app-editar-perfil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, Icono],
  templateUrl: './editar-perfil.html',
  styleUrl: './editar-perfil.scss',
})
export class EditarPerfil {
  private readonly perfil = inject(PerfilServicio);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastServicio);
  private readonly i18n = inject(TranslocoService);

  protected readonly estado = this.perfil.estado;
  protected readonly semestres = SEMESTRES;
  protected readonly modalidades = MODALIDADES_PERFIL;
  protected readonly disponibilidades = DISPONIBILIDADES;
  protected readonly carreras = signal<Carrera[]>([]);
  protected readonly enviando = signal(false);
  protected readonly errorEnvio = signal(false);
  protected readonly intentado = signal(false);

  protected readonly formulario = new FormGroup({
    tipo: new FormControl<TipoPersona>('ESTUDIANTE', { nonNullable: true, validators: Validators.required }),
    carrera: new FormControl('', { nonNullable: true, validators: Validators.required }),
    semestre: new FormControl<number | null>(null),
    modalidades: new FormControl<ModalidadPerfil[]>([], { nonNullable: true }),
    disponibilidad: new FormControl<Disponibilidad | null>(null),
  });

  private readonly tipo = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });
  protected readonly esEstudiante = computed(() => this.tipo() === 'ESTUDIANTE');
  private cargado = false;

  constructor() {
    this.formulario.controls.tipo.valueChanges.pipe(takeUntilDestroyed()).subscribe((tipo) => {
      const semestre = this.formulario.controls.semestre;
      semestre.setValidators(tipo === 'ESTUDIANTE' ? Validators.required : null);
      if (tipo !== 'ESTUDIANTE') semestre.setValue(null);
      semestre.updateValueAndValidity();
    });
    // Llena el formulario una sola vez, cuando llega el perfil (puede llegar después de abrir la pantalla).
    effect(() => {
      const d = this.estado()?.datos;
      if (!d || this.cargado) return;
      this.cargado = true;
      this.formulario.setValue({
        tipo: d.tipo,
        carrera: d.carrera.codigo,
        semestre: d.semestre,
        modalidades: [...d.modalidades],
        disponibilidad: d.disponibilidad,
      });
    });
    this.perfil.carreras().then(
      (c) => this.carreras.set(c),
      () => this.errorEnvio.set(true),
    );
  }

  protected marcada(m: ModalidadPerfil): boolean {
    return this.formulario.controls.modalidades.value.includes(m);
  }

  protected alternar(m: ModalidadPerfil): void {
    const actual = this.formulario.controls.modalidades.value;
    this.formulario.controls.modalidades.setValue(
      actual.includes(m) ? actual.filter((x) => x !== m) : [...actual, m],
    );
  }

  protected error(campo: keyof typeof this.formulario.controls): boolean {
    return this.intentado() && this.formulario.controls[campo].invalid;
  }

  protected async guardar(): Promise<void> {
    this.intentado.set(true);
    if (this.formulario.invalid || this.enviando()) return;
    const v = this.formulario.getRawValue();
    this.enviando.set(true);
    this.errorEnvio.set(false);
    try {
      await this.perfil.actualizar({
        tipo: v.tipo,
        carrera: v.carrera,
        semestre: v.tipo === 'ESTUDIANTE' ? v.semestre : null,
        modalidades: v.modalidades,
        disponibilidad: v.disponibilidad,
      });
      this.toasts.mostrar(this.i18n.translate('perfil.editar.guardado'));
      void this.router.navigate(['/perfil']);
    } catch {
      this.errorEnvio.set(true);
    } finally {
      this.enviando.set(false);
    }
  }
}
