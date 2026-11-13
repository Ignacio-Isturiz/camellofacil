import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Solo se aceptan correos institucionales de UNAULA (el backend debe repetir esta validación). */
const CORREO_UNAULA = /^[^\s@]+@unaula\.edu\.co$/i;

export function correoUnaula(control: AbstractControl<string>): ValidationErrors | null {
  const valor = (control.value ?? '').trim();
  if (valor === '') return null; // lo cubre `required`
  return CORREO_UNAULA.test(valor) ? null : { correoDominio: true };
}

/** Reglas de la contraseña de registro (se muestran como lista mientras se escribe). */
export const REGLAS_CONTRASENA = [
  { clave: 'longitud', cumple: (v: string) => v.length >= 8 },
  { clave: 'mayuscula', cumple: (v: string) => /\p{Lu}/u.test(v) },
  { clave: 'numero', cumple: (v: string) => /\d/.test(v) },
  { clave: 'minuscula', cumple: (v: string) => /\p{Ll}/u.test(v) },
] as const;

export function contrasenaFuerte(control: AbstractControl<string>): ValidationErrors | null {
  const valor = control.value ?? '';
  if (valor === '') return null;
  return REGLAS_CONTRASENA.every((r) => r.cumple(valor)) ? null : { contrasenaDebil: true };
}
