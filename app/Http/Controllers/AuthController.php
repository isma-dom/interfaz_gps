<?php

namespace App\Http\Controllers;

use App\Http\Requests\IniciarSesionRequest;
use App\Models\Usuario;
use App\Services\BloqueoDeAcceso;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;
use Illuminate\View\View;

/** C-01. Inicio y cierre de sesión web. */
class AuthController extends Controller
{
    /**
     * Se compara contra este hash cuando el correo no existe, para que la
     * respuesta tarde lo mismo y no revele qué cuentas hay.
     */
    private const HASH_FICTICIO = '$2y$12$X634LEVf9DiAHn7Z.jwSE.JBQOwhJuDVMyo7W1gjhgMSQ.SlsayRK';

    public function mostrarLogin(Request $request): View
    {
        return view('auth.login', ['sesionExpirada' => $request->boolean('expirada')]);
    }

    public function login(IniciarSesionRequest $request, BloqueoDeAcceso $bloqueo): RedirectResponse
    {
        $correo = $request->validated('correo');

        if ($bloqueo->estaBloqueado($correo)) {
            throw $this->errorDeBloqueo($bloqueo, $correo);
        }

        $usuario = Usuario::where('correo', $correo)->first();
        $contrasenaValida = Hash::check($request->validated('password'), $usuario->password ?? self::HASH_FICTICIO);

        if (! $usuario || ! $usuario->activo || ! $contrasenaValida) {
            $bloqueo->registrarFallo($correo);

            if ($bloqueo->estaBloqueado($correo)) {
                throw $this->errorDeBloqueo($bloqueo, $correo);
            }

            throw ValidationException::withMessages(['correo' => 'Correo o contraseña incorrectos.']);
        }

        $bloqueo->limpiar($correo);
        Auth::login($usuario);
        $request->session()->regenerate();

        return redirect()->intended($usuario->rutaInicio());
    }

    public function logout(Request $request): RedirectResponse
    {
        Auth::guard('web')->logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect()->route('login');
    }

    /** La respuesta de esta petición renueva la sesión («Seguir conectado»). */
    public function mantenerSesion(): Response
    {
        return response()->noContent();
    }

    private function errorDeBloqueo(BloqueoDeAcceso $bloqueo, string $correo): ValidationException
    {
        $minutos = $bloqueo->minutosRestantes($correo);

        return ValidationException::withMessages([
            'correo' => 'Demasiados intentos fallidos. Intenta de nuevo en '.$minutos.' '.($minutos === 1 ? 'minuto' : 'minutos').'.',
        ]);
    }
}
