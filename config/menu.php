<?php

use App\Models\Rol;

/*
| Menú y permisos por pantalla (AD 2.2.7), definidos en un solo lugar:
| routes/web.php aplica estos roles a cada ruta y el layout los usa para el menú.
*/

$todos = [Rol::ADMINISTRADOR, Rol::EJECUTIVO, Rol::CONSULTA];
$administradorYEjecutivo = [Rol::ADMINISTRADOR, Rol::EJECUTIVO];
$administrador = [Rol::ADMINISTRADOR];

return [
    ['ruta' => 'tablero', 'url' => 'tablero', 'etiqueta' => 'Tablero', 'icono' => 'bar-chart-line', 'roles' => $administradorYEjecutivo],
    ['ruta' => 'equipos', 'url' => 'equipos', 'etiqueta' => 'Equipos', 'icono' => 'pc-display', 'roles' => $todos],
    ['ruta' => 'escaneo', 'url' => 'escaneo', 'etiqueta' => 'Escaneo de QR', 'icono' => 'qr-code-scan', 'roles' => $todos],
    ['ruta' => 'resguardos', 'url' => 'resguardos', 'etiqueta' => 'Resguardos', 'icono' => 'file-earmark-person', 'roles' => $todos],
    ['ruta' => 'importaciones', 'url' => 'importaciones', 'etiqueta' => 'Importación y cuarentena', 'icono' => 'file-earmark-arrow-up', 'roles' => $administrador],
    ['ruta' => 'reportes', 'url' => 'reportes', 'etiqueta' => 'Reportes', 'icono' => 'file-earmark-text', 'roles' => $todos],
    ['ruta' => 'catalogos', 'url' => 'catalogos', 'etiqueta' => 'Catálogos', 'icono' => 'collection', 'roles' => $administrador],
    ['ruta' => 'usuarios.index', 'url' => 'usuarios', 'etiqueta' => 'Usuarios', 'icono' => 'people', 'roles' => $administrador],
    ['ruta' => 'bitacora', 'url' => 'bitacora', 'etiqueta' => 'Bitácora', 'icono' => 'journal-text', 'roles' => $administrador],
];
