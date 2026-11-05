<?php

use App\Http\Middleware\VerificarRol;
use App\Http\Middleware\VerificarUsuarioActivo;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->alias([
            'rol' => VerificarRol::class,
            'activo' => VerificarUsuarioActivo::class,
        ]);

        $middleware->redirectUsersTo(fn (Request $request) => $request->user()->rutaInicio());
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        // Una página abierta con el token vencido vuelve al login en lugar de mostrar el error 419.
        $exceptions->respond(function (Response $response, Throwable $e, Request $request) {
            if ($response->getStatusCode() === 419 && ! $request->expectsJson()) {
                return redirect()->route('login')->with('aviso', 'Tu sesión expiró por inactividad.');
            }

            return $response;
        });
    })->create();
