<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Symfony\Component\HttpFoundation\Response;

/** Un usuario desactivado pierde el acceso en su siguiente petición. */
class VerificarUsuarioActivo
{
    public function handle(Request $request, Closure $next): Response
    {
        if ($request->user() && ! $request->user()->activo) {
            Auth::guard('web')->logout();
            $request->session()->invalidate();
            $request->session()->regenerateToken();

            if ($request->expectsJson()) {
                abort(401, 'Tu acceso fue desactivado.');
            }

            return redirect()->route('login')
                ->with('aviso', 'Tu acceso fue desactivado. Pide ayuda a Soporte Técnico.');
        }

        return $next($request);
    }
}
