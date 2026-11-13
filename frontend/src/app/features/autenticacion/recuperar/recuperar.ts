import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';
import { CampoCorreo } from '../campos/campo-correo';
import { MarcoAutenticacion } from '../marco-autenticacion/marco-autenticacion';
import { correoUnaula } from '../validadores';

/** /recuperar — pide el correo para enviar el enlace de restablecimiento. */
@Component({
  selector: 'app-recuperar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, Icono, MarcoAutenticacion, CampoCorreo],
  templateUrl: './recuperar.html',
})
export class Recuperar {
  protected readonly formulario = new FormGroup({
    correo: new FormControl('', { nonNullable: true, validators: [Validators.required, correoUnaula] }),
  });

  protected readonly intentado = signal(false);
  protected readonly enviado = signal(false);

  protected enviar(): void {
    this.intentado.set(true);
    if (this.formulario.invalid) return;
    // Pendiente: pedir el enlace al backend. El mensaje es el mismo exista o no la cuenta (no revela quién está registrado).
    this.enviado.set(true);
  }
}
