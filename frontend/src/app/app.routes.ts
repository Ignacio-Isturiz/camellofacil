import { Routes } from '@angular/router';

import { exigirSesion } from './core/sesion/exigir-sesion';
import { irAInicioConSesion } from './core/sesion/ir-a-inicio-con-sesion';

// Una carpeta features/ por módulo del backend, con carga diferida por función.
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [irAInicioConSesion],
    title: 'Camello Fácil · Prácticas y empleo para la comunidad UNAULA',
    loadComponent: () => import('./features/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'conexion',
    loadComponent: () => import('./features/conexion/conexion').then((m) => m.Conexion),
  },
  // Único ingreso: la cuenta Microsoft de UNAULA (no hay registro ni contraseñas propias).
  {
    path: 'login',
    canActivate: [irAInicioConSesion],
    title: 'Inicia sesión · Camello Fácil',
    loadComponent: () => import('./features/autenticacion/login/login').then((m) => m.Login),
  },
  // Enlaces viejos a /registro llevan al ingreso con Microsoft.
  { path: 'registro', redirectTo: 'login' },
  // Inicio de quien ya inició sesión.
  {
    path: 'inicio',
    canActivate: [exigirSesion],
    loadComponent: () => import('./shared/marco-aplicacion/marco-aplicacion').then((m) => m.MarcoAplicacion),
    children: [
      {
        path: '',
        title: 'Inicio · Camello Fácil',
        loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
      },
    ],
  },
  // Pantallas de quien inició sesión.
  {
    path: 'vacantes',
    canActivate: [exigirSesion],
    loadComponent: () => import('./shared/marco-aplicacion/marco-aplicacion').then((m) => m.MarcoAplicacion),
    children: [
      {
        path: '',
        title: 'Vacantes · Camello Fácil',
        loadComponent: () => import('./features/vacantes/lista/lista-vacantes').then((m) => m.ListaVacantes),
      },
      {
        path: ':id',
        title: 'Detalle de la vacante · Camello Fácil',
        loadComponent: () => import('./features/vacantes/detalle/detalle-vacante').then((m) => m.DetalleVacante),
      },
    ],
  },
  // Módulo «Hoja de vida». Las demás pantallas (subir, revisar, reorganizar, plantillas, versiones, editor)
  // se agregan como hijas de esta ruta, dentro del mismo marco.
  {
    path: 'hoja-de-vida',
    canActivate: [exigirSesion],
    loadComponent: () => import('./shared/marco-aplicacion/marco-aplicacion').then((m) => m.MarcoAplicacion),
    children: [
      {
        path: '',
        title: 'Mi hoja de vida · Camello Fácil',
        loadComponent: () =>
          import('./features/hoja-de-vida/inicio/hoja-de-vida-inicio').then((m) => m.HojaDeVidaInicio),
      },
      {
        path: 'subir',
        title: 'Sube tu hoja de vida · Camello Fácil',
        loadComponent: () => import('./features/hoja-de-vida/subir/subir').then((m) => m.Subir),
      },
      {
        path: 'plantillas',
        title: 'Plantillas · Camello Fácil',
        loadComponent: () => import('./features/hoja-de-vida/plantillas/plantillas').then((m) => m.Plantillas),
      },
      {
        path: 'versiones',
        title: 'Mis versiones · Camello Fácil',
        loadComponent: () => import('./features/hoja-de-vida/versiones/versiones').then((m) => m.Versiones),
      },
      {
        path: 'reorganizar',
        title: 'Reorganizar tu hoja · Camello Fácil',
        loadComponent: () => import('./features/hoja-de-vida/reorganizar/reorganizar').then((m) => m.Reorganizar),
      },
      {
        path: 'editor',
        title: 'Editor de hoja de vida · Camello Fácil',
        loadComponent: () => import('./features/hoja-de-vida/editor/editor').then((m) => m.Editor),
      },
      {
        path: 'revisar',
        title: 'Revisa lo que leímos · Camello Fácil',
        loadComponent: () => import('./features/hoja-de-vida/revisar/revisar').then((m) => m.Revisar),
      },
    ],
  },
  // Mi perfil: datos, lo que falta completar (hoja de vida base) y privacidad.
  {
    path: 'perfil',
    canActivate: [exigirSesion],
    loadComponent: () => import('./shared/marco-aplicacion/marco-aplicacion').then((m) => m.MarcoAplicacion),
    children: [
      {
        path: '',
        title: 'Mi perfil · Camello Fácil',
        loadComponent: () => import('./features/perfil/mi-perfil/mi-perfil').then((m) => m.MiPerfil),
      },
      {
        path: 'editar',
        title: 'Editar perfil · Camello Fácil',
        loadComponent: () => import('./features/perfil/editar-perfil/editar-perfil').then((m) => m.EditarPerfil),
      },
    ],
  },
  // Documentos legales (públicos). Borrador 1.0, pendiente de revisión legal.
  {
    path: 'terminos',
    title: 'Términos y condiciones · Camello Fácil',
    data: { documento: 'terminos' },
    loadComponent: () => import('./features/legal/documento-legal').then((m) => m.DocumentoLegal),
  },
  {
    path: 'privacidad',
    title: 'Política de privacidad · Camello Fácil',
    data: { documento: 'privacidad' },
    loadComponent: () => import('./features/legal/documento-legal').then((m) => m.DocumentoLegal),
  },
  // Historial de clics en «Postular».
  {
    path: 'postulaciones',
    canActivate: [exigirSesion],
    loadComponent: () => import('./shared/marco-aplicacion/marco-aplicacion').then((m) => m.MarcoAplicacion),
    children: [
      {
        path: '',
        title: 'Mis postulaciones · Camello Fácil',
        loadComponent: () => import('./features/postulaciones/postulaciones').then((m) => m.Postulaciones),
      },
    ],
  },
  // Aún no existe /contacto: vuelve al inicio. Quitar al agregarla.
  { path: '**', redirectTo: '' },
];
