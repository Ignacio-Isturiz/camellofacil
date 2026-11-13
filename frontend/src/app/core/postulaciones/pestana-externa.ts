/**
 * Abre la página original de una vacante en otra pestaña.
 *
 * Los navegadores solo permiten abrir una pestaña dentro del clic de la persona, no después de esperar al
 * servidor. Por eso se abre en blanco al instante (`abrirEnBlanco`, llamarlo dentro del manejador del clic)
 * y se le asigna la dirección cuando llega (`completar`).
 */
export interface PestanaPendiente {
  completar(url: string): boolean;
  cancelar(): void;
}

/** Solo se abren direcciones http(s): la URL viene de una fuente externa y no se confía en ella. */
export function urlSegura(url: string): string | null {
  try {
    const u = new URL(url);
    return u.protocol === 'https:' || u.protocol === 'http:' ? u.toString() : null;
  } catch {
    return null;
  }
}

export function abrirEnBlanco(): PestanaPendiente {
  const ventana = window.open('about:blank', '_blank');
  if (ventana) ventana.opener = null;
  return {
    completar(url: string): boolean {
      const segura = urlSegura(url);
      if (!segura) {
        ventana?.close();
        return false;
      }
      if (ventana) ventana.location.href = segura;
      else window.open(segura, '_blank', 'noopener,noreferrer');
      return true;
    },
    cancelar(): void {
      ventana?.close();
    },
  };
}
