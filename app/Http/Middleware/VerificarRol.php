<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * C-04. Uso: ->middleware('rol:Administrador,Ejecutivo').
 *
 * Laravel responde al 403 con JSON si la petición espera JSON y con la vista
 * errors/403 en caso contrario, así que sirve también para la API.
 */
class VerificarRol
{
    public function handle(Request $request, Closure $next, string ...$roles): Response
    {
        if (! $request->user()?->tieneRol(...$roles)) {
            abort(403, 'No tienes permiso para realizar esta acción.');
        }

        return $next($request);
    }
}
