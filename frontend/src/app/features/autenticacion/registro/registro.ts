import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Sesion } from '../../../core/sesion/sesion';
import { Icono } from '../../../shared/icono/icono';
import { LogoMicrosoft } from '../../../shared/logo-microsoft/logo-microsoft';
import { CampoContrasena } from '../campos/campo-contrasena';
import { CampoCorreo } from '../campos/campo-correo';
import { MarcoAutenticacion } from '../marco-autenticacion/marco-autenticacion';
import { contrasenaFuerte, correoUnaula, REGLAS_CONTRASENA } from '../validadores';

/** /registro — paso 1 de 3: solo con correo @unaula.edu.co (o con Microsoft), contraseña y términos. */
@Component({
  selector: 'app-registro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
    Icono,
    LogoMicrosoft,
    MarcoAutenticacion,
    CampoCorreo,
    CampoContrasena,
  ],
  templateUrl: './registro.html',
})
export class Registro {
  private readonly router = inject(Router);
  private readonly sesion = inject(Sesion);

  protected readonly formulario = new FormGroup({
    correo: new FormControl('', { nonNullable: true, validators: [Validators.required, correoUnaula] }),
    contrasena: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, contrasenaFuerte],
    }),
    terminos: new FormControl(false, { nonNullable: true, validators: [Validators.requiredTrue] }),
  });

  protected readonly intentado = signal(false);

  private readonly contrasena = toSignal(this.formulario.controls.contrasena.valueChanges, { initialValue: '' });
  private readonly aceptado = toSignal(this.formulario.controls.terminos.valueChanges, { initialValue: false });

  /** Cada regla con su estado, para pintar la lista mientras se escribe. */
  protected readonly reglas = computed(() =>
    REGLAS_CONTRASENA.map((r) => ({ clave: r.clave, cumple: r.cumple(this.contrasena()) })),
  );
  /** Dos barras: la primera con la mitad de las reglas, la segunda con todas. */
  protected readonly nivel = computed(() => {
    const cumplidas = this.reglas().filter((r) => r.cumple).length;
    return cumplidas === REGLAS_CONTRASENA.length ? 2 : cumplidas >= 2 ? 1 : 0;
  });

  protected readonly errorTerminos = computed(() => this.intentado() && !this.aceptado());

  protected enviar(): void {
    this.intentado.set(true);
    if (this.formulario.invalid) return;
    // El correo viaja en el estado de la navegación (no en la URL) para mostrarlo en el paso 2.
    // Pendiente: crear la cuenta en el backend, que es quien envía el código y valida el dominio.
    void this.router.navigate(['/registro/verificar'], {
      state: { correo: this.formulario.controls.correo.value.trim() },
    });
  }

  protected conMicrosoft(): void {
    // Pendiente: flujo de Microsoft del backend (solo cuentas @unaula.edu.co). Mientras tanto, sesión simulada.
    this.sesion.iniciarSimulada();
    void this.router.navigate(['/inicio']);
  }
}
