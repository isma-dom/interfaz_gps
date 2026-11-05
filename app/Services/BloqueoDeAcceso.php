<?php

namespace App\Services;

use Illuminate\Support\Facades\RateLimiter;

/**
 * Bloqueo temporal tras 5 intentos fallidos con la misma cuenta (C-01, CP-03).
 *
 * La clave es solo el correo normalizado, sin la IP: cambiar de equipo no debe
 * reiniciar la cuenta. Existe o no la cuenta, se comporta igual, para no revelar
 * qué correos están registrados.
 */
class BloqueoDeAcceso
{
    public function estaBloqueado(string $correo): bool
    {
        return RateLimiter::attempts($this->claveBloqueo($correo)) > 0
            && RateLimiter::availableIn($this->claveBloqueo($correo)) > 0;
    }

    public function minutosRestantes(string $correo): int
    {
        return (int) max(1, ceil(RateLimiter::availableIn($this->claveBloqueo($correo)) / 60));
    }

    public function registrarFallo(string $correo): void
    {
        $segundos = config('sigit.bloqueo.minutos') * 60;
        $fallos = RateLimiter::hit($this->claveFallos($correo), $segundos);

        if ($fallos >= config('sigit.bloqueo.intentos')) {
            // El plazo cuenta desde el último intento, no desde el primero.
            RateLimiter::clear($this->claveFallos($correo));
            RateLimiter::hit($this->claveBloqueo($correo), $segundos);
        }
    }

    public function limpiar(string $correo): void
    {
        RateLimiter::clear($this->claveFallos($correo));
        RateLimiter::clear($this->claveBloqueo($correo));
    }

    private function claveFallos(string $correo): string
    {
        return 'login-fallos:'.sha1($correo);
    }

    private function claveBloqueo(string $correo): string
    {
        return 'login-bloqueo:'.sha1($correo);
    }
}
