import { Routes } from '@angular/router';

// Una carpeta features/ por módulo del backend, con carga diferida por función.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'conexion' },
  {
    path: 'conexion',
    loadComponent: () => import('./features/conexion/conexion').then((m) => m.Conexion),
  },
];
