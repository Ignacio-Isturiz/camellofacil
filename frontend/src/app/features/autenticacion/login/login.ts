import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Sesion } from '../../../core/sesion/sesion';
import { Icono } from '../../../shared/icono/icono';
import { LogoMicrosoft } from '../../../shared/logo-microsoft/logo-microsoft';
import { CampoContrasena } from '../campos/campo-contrasena';
import { CampoCorreo } from '../campos/campo-correo';
import { MarcoAutenticacion } from '../marco-autenticacion/marco-autenticacion';
import { correoUnaula } from '../validadores';

/** /login — ingreso con correo institucional y contraseña, o con Microsoft. */
@Component({
  selector: 'app-login',
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
  templateUrl: './login.html',
})
export class Login {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly formulario = new FormGroup({
    correo: new FormControl('', { nonNullable: true, validators: [Validators.required, correoUnaula] }),
    contrasena: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    mantener: new FormControl(true, { nonNullable: true }),
  });

  protected readonly intentado = signal(false);

  protected enviar(): void {
    this.intentado.set(true);
    if (this.formulario.invalid) return;
    // SESIÓN SIMULADA (ver core/sesion): no valida la contraseña. Reemplazar por el login real del backend.
    this.sesion.iniciarSimulada(this.formulario.controls.correo.value);
    void this.router.navigate(['/inicio']);
  }

  protected conMicrosoft(): void {
    // Pendiente: flujo de Microsoft del backend. Mientras tanto, sesión simulada.
    this.sesion.iniciarSimulada();
    void this.router.navigate(['/inicio']);
  }
}
