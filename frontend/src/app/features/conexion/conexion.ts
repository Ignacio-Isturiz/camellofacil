import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Api } from '../../core/api/api';
import { comprobar } from '../../core/api/fn/conexion-controller/comprobar';

type Estado = 'cargando' | 'ok' | 'error';

/** Pantalla temporal para comprobar que el frontend habla con el backend. */
@Component({
  selector: 'app-conexion',
  imports: [TranslocoPipe],
  templateUrl: './conexion.html',
  styleUrl: './conexion.scss',
})
export class Conexion implements OnInit {
  private readonly api = inject(Api);

  protected readonly estado = signal<Estado>('cargando');
  protected readonly mensaje = signal('');

  ngOnInit(): void {
    void this.comprobarConexion();
  }

  protected async comprobarConexion(): Promise<void> {
    this.estado.set('cargando');
    try {
      const respuesta = await this.api.invoke(comprobar);
      this.mensaje.set(respuesta.mensaje ?? '');
      this.estado.set('ok');
    } catch {
      this.estado.set('error');
    }
  }
}
