import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { Icono } from '../../../shared/icono/icono';
import { EsqueletoVacante } from '../esqueleto/esqueleto-vacante';
import { TarjetaVacante } from '../tarjeta-vacante/tarjeta-vacante';
import { CARRERAS, Modalidad, MODALIDADES, Vacante } from '../vacante';
import { VacantesServicio } from '../vacantes-servicio';

type Fecha = 'cualquiera' | 'hoy' | 'semana' | 'mes';
type Orden = 'relevantes' | 'recientes';

const POR_PAGINA = 4;
const LIMITE_DIAS: Record<Fecha, number> = { cualquiera: Infinity, hoy: 0, semana: 7, mes: 30 };

/** /vacantes — búsqueda, pestañas por tipo, filtros y lista. Todo el filtrado se hace en el cliente sobre los datos de muestra. */
@Component({
  selector: 'app-lista-vacantes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, Icono, TarjetaVacante, EsqueletoVacante],
  templateUrl: './lista-vacantes.html',
  styleUrl: './lista-vacantes.scss',
})
export class ListaVacantes {
  private readonly servicio = inject(VacantesServicio);
  protected readonly guardadas = this.servicio.guardadas;
  protected readonly estado = this.servicio.estado;
  protected readonly esqueletos = [1, 2, 3, 4];

  protected readonly carreras = CARRERAS;
  protected readonly modalidadesPosibles = MODALIDADES;
  protected readonly fechas: readonly Fecha[] = ['cualquiera', 'hoy', 'semana', 'mes'];

  // Estado de la búsqueda y los filtros. Por defecto: todas las carreras y la última semana.
  protected readonly consulta = signal('');
  private readonly busqueda = signal('');
  protected readonly carrera = signal<string>('todas');
  protected readonly modalidades = signal<ReadonlySet<Modalidad>>(new Set(MODALIDADES));
  protected readonly fecha = signal<Fecha>('semana');
  protected readonly orden = signal<Orden>('relevantes');
  protected readonly visibles = signal(POR_PAGINA);
  protected readonly filtrosAbiertos = signal(false);

  /** Solo se muestran prácticas (la plataforma es para practicantes). Los demás filtros se aplican encima. */
  private readonly candidatas = computed(() => {
    const texto = normalizar(this.busqueda());
    const ocultas = this.servicio.ocultas();
    return this.servicio.todas().filter(
      (v) =>
        v.tipo === 'practica' &&
        !ocultas.has(v.id) &&
        (texto === '' || coincideTexto(v, texto)) &&
        (this.carrera() === 'todas' || v.carreras.includes(this.carrera())) &&
        this.modalidades().has(v.modalidad) &&
        v.dias <= LIMITE_DIAS[this.fecha()],
    );
  });

  private readonly resultados = computed(() => {
    return [...this.candidatas()].sort((a, b) =>
      this.orden() === 'recientes' ? a.dias - b.dias : b.coincidencia - a.coincidencia,
    );
  });

  protected readonly total = computed(() => this.resultados().length);
  protected readonly mostradas = computed(() => this.resultados().slice(0, this.visibles()));
  protected readonly hayMas = computed(() => this.total() > this.visibles());

  protected buscar(evento: Event): void {
    evento.preventDefault();
    this.busqueda.set(this.consulta());
    this.visibles.set(POR_PAGINA);
  }

  /** Cambia un filtro y vuelve a la primera página de resultados. */
  protected cambiar<T>(estado: { set: (v: T) => void }, valor: T): void {
    estado.set(valor);
    this.visibles.set(POR_PAGINA);
  }

  protected alternarModalidad(m: Modalidad): void {
    this.cambiar(this.modalidades, alternar(this.modalidades(), m));
  }

  protected limpiar(): void {
    this.consulta.set('');
    this.busqueda.set('');
    this.carrera.set('todas');
    this.modalidades.set(new Set(MODALIDADES));
    this.fecha.set('cualquiera');
    this.visibles.set(POR_PAGINA);
  }

  protected cargarMas(): void {
    this.visibles.update((n) => n + POR_PAGINA);
  }

  protected guardar(id: string): void {
    this.servicio.alternarGuardada(id);
  }

  protected ocultar(id: string): void {
    this.servicio.ocultar(id);
  }

  protected valor(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLSelectElement).value;
  }
}

function alternar<T>(conjunto: ReadonlySet<T>, valor: T): ReadonlySet<T> {
  const copia = new Set(conjunto);
  if (!copia.delete(valor)) copia.add(valor);
  return copia;
}

/** Minúsculas y sin tildes, para que "analisis" encuentre "análisis". */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}

function coincideTexto(v: Vacante, texto: string): boolean {
  return normalizar(`${v.cargo} ${v.empresa} ${v.descripcion}`).includes(texto);
}
