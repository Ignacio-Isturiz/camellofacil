import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChildren,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';
import { MarcoAutenticacion } from '../marco-autenticacion/marco-autenticacion';

const LONGITUD = 6;
const ESPERA_REENVIO_S = 30;

/** /registro/verificar — paso 2 de 3: código de 6 dígitos enviado al correo institucional. */
@Component({
  selector: 'app-verificar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslocoPipe, Icono, MarcoAutenticacion],
  templateUrl: './verificar.html',
})
export class Verificar {
  private readonly destruir = inject(DestroyRef);

  /** El correo llega en el estado de la navegación desde /registro; si se recarga la página no está. */
  protected readonly correo: string | null =
    (inject(Router).currentNavigation()?.extras.state?.['correo'] as string | undefined) ?? null;

  protected readonly posiciones = Array.from({ length: LONGITUD }, (_, i) => i);
  protected readonly digitos = signal<string[]>(Array<string>(LONGITUD).fill(''));
  protected readonly completo = computed(() => this.digitos().every((d) => d !== ''));
  protected readonly espera = signal(0);
  protected readonly reenviado = signal(false);

  private readonly casillas = viewChildren<ElementRef<HTMLInputElement>>('casilla');
  private temporizador: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.destruir.onDestroy(() => this.detenerEspera());
  }

  protected escribir(i: number, evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const digito = entrada.value.replace(/\D/g, '').slice(-1);
    entrada.value = digito; // descarta cualquier carácter que no sea un dígito
    this.poner(i, digito);
    if (digito && i < LONGITUD - 1) this.enfocar(i + 1);
    if (this.completo()) this.verificar();
  }

  protected teclear(i: number, evento: KeyboardEvent): void {
    if (evento.key === 'Backspace' && this.digitos()[i] === '' && i > 0) {
      this.enfocar(i - 1);
    } else if (evento.key === 'ArrowLeft' && i > 0) {
      this.enfocar(i - 1);
    } else if (evento.key === 'ArrowRight' && i < LONGITUD - 1) {
      this.enfocar(i + 1);
    }
  }

  protected pegar(evento: ClipboardEvent): void {
    const texto = (evento.clipboardData?.getData('text') ?? '').replace(/\D/g, '').slice(0, LONGITUD);
    if (!texto) return;
    evento.preventDefault();
    this.digitos.set(Array.from({ length: LONGITUD }, (_, i) => texto[i] ?? ''));
    this.enfocar(Math.min(texto.length, LONGITUD - 1));
    if (this.completo()) this.verificar();
  }

  protected reenviar(): void {
    if (this.espera() > 0) return;
    // Pendiente: pedir un código nuevo al backend.
    this.reenviado.set(true);
    this.espera.set(ESPERA_REENVIO_S);
    this.detenerEspera();
    this.temporizador = setInterval(() => {
      this.espera.update((s) => s - 1);
      if (this.espera() <= 0) this.detenerEspera();
    }, 1000);
  }

  private verificar(): void {
    // Pendiente: enviar el código al backend y pasar al paso 3 (completar perfil) cuando existan.
  }

  private poner(i: number, valor: string): void {
    this.digitos.update((actual) => actual.map((d, j) => (j === i ? valor : d)));
  }

  private enfocar(i: number): void {
    const casilla = this.casillas()[i]?.nativeElement;
    casilla?.focus();
    casilla?.select();
  }

  private detenerEspera(): void {
    if (this.temporizador) clearInterval(this.temporizador);
    this.temporizador = null;
  }
}
