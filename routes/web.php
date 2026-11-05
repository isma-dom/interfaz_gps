<?php

use App\Http\Controllers\AuthController;
use App\Http\Controllers\EnConstruccionController;
use App\Http\Controllers\UsuarioController;
use App\Models\Rol;
use Illuminate\Support\Facades\Route;

Route::middleware('guest')->group(function () {
    Route::get('login', [AuthController::class, 'mostrarLogin'])->name('login');
    Route::post('login', [AuthController::class, 'login']);
});

// no_store: tras cerrar sesión, el botón Atrás del navegador no muestra datos.
Route::middleware(['auth', 'activo', 'cache.headers:no_store;private'])->group(function () {
    Route::get('/', fn () => redirect(auth()->user()->rutaInicio()))->name('inicio');

    Route::post('logout', [AuthController::class, 'logout'])->name('logout');
    Route::post('sesion/mantener', [AuthController::class, 'mantenerSesion'])->name('sesion.mantener');

    foreach (config('menu') as $entrada) {
        if ($entrada['ruta'] !== 'usuarios.index') {
            Route::get($entrada['url'], EnConstruccionController::class)
                ->name($entrada['ruta'])
                ->middleware('rol:'.implode(',', $entrada['roles']));
        }
    }

    Route::middleware('rol:'.Rol::ADMINISTRADOR)->group(function () {
        Route::resource('usuarios', UsuarioController::class)->except(['show', 'destroy']);
        Route::patch('usuarios/{usuario}/desactivar', [UsuarioController::class, 'desactivar'])->name('usuarios.desactivar');
        Route::patch('usuarios/{usuario}/reactivar', [UsuarioController::class, 'reactivar'])->name('usuarios.reactivar');
    });
});
