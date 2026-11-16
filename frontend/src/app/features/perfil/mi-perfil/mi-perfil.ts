import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { HojaDeVidaServicio } from '../../../core/hoja-de-vida/hoja-de-vida.servicio';
import { PerfilServicio } from '../../../core/perfil/perfil.servicio';
import { Sesion } from '../../../core/sesion/sesion';
import { FechaCortaPipe } from '../../../shared/fecha-corta/fecha-corta.pipe';
import { Icono } from '../../../shared/icono/icono';
import { ToastServicio } from '../../../shared/toast/toast.servicio';

/** /perfil — quién es la persona, qué le falta completar y sus datos y consentimientos (Ley 1581). */
@Component({
  selector: 'app-mi-perfil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, FechaCortaPipe],
  templateUrl: './mi-perfil.html',
  styleUrl: './mi-perfil.scss',
})
export class MiPerfil {
  private readonly perfil = inject(PerfilServicio);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastServicio);
  private readonly i18n = inject(TranslocoService);
  private readonly hojas = inject(HojaDeVidaServicio);

  protected readonly estado = this.perfil.estado;
  protected readonly datos = computed(() => this.estado()?.datos ?? null);
  protected readonly iniciales = computed(() => this.sesion.usuario()?.iniciales ?? '');

  protected readonly tienePreferencias = computed(() => {
    const d = this.datos();
    return !!d && (d.modalidades.length > 0 || !!d.disponibilidad);
  });

  constructor() {
    this.hojas.cargarResumen();
  }

  /** Lo que falta para sacarle provecho a la plataforma. La hoja base cuenta cuando ya está confirmada. */
  protected readonly pendientes = computed(() => {
    const hojaBase = this.hojas.resumen()?.estado === 'confirmada';
    return [
      { clave: 'hoja', hecho: hojaBase, ruta: '/hoja-de-vida/subir' },
      { clave: 'preferencias', hecho: this.tienePreferencias(), ruta: '/perfil/editar' },
    ];
  });
  protected readonly hechos = computed(() => this.pendientes().filter((p) => p.hecho).length + 1);
  protected readonly total = computed(() => this.pendientes().length + 1);

  protected readonly confirmandoRevocar = signal(false);
  protected readonly cambiandoIa = signal(false);

  protected async cambiarIa(activa: boolean): Promise<void> {
    this.cambiandoIa.set(true);
    try {
      await this.perfil.cambiarAnalisisIa(activa);
      this.confirmandoRevocar.set(false);
      this.toasts.mostrar(this.i18n.translate(activa ? 'perfil.privacidad.iaActivada' : 'perfil.privacidad.iaRevocada'));
    } catch {
      this.toasts.mostrar(this.i18n.translate('perfil.errores.guardar'));
    } finally {
      this.cambiandoIa.set(false);
    }
  }

  protected cerrarSesion(): void {
    this.perfil.olvidar();
    void this.sesion.cerrar();
    void this.router.navigate(['/']);
  }
}
