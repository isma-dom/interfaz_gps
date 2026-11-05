<?php

namespace App\Services;

use App\Models\Rol;
use App\Models\Usuario;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/** Reglas para gestionar usuarios (C-02): nadie deja al sistema sin administradores. */
class UsuarioService
{
    /** @param array<string, mixed> $datos */
    public function crear(array $datos): Usuario
    {
        return Usuario::create($this->normalizar($datos));
    }

    /** @param array<string, mixed> $datos */
    public function actualizar(Usuario $usuario, array $datos, Usuario $actor): Usuario
    {
        $datos = $this->normalizar($datos);
        $dejaDeSerAdministrador = (int) $datos['rol_id'] !== (int) $usuario->rol_id
            && $usuario->tieneRol(Rol::ADMINISTRADOR);

        if ($dejaDeSerAdministrador && $usuario->is($actor)) {
            throw ValidationException::withMessages([
                'rol_id' => 'No puedes quitarte el rol de Administrador.',
            ]);
        }

        return DB::transaction(function () use ($usuario, $datos, $dejaDeSerAdministrador) {
            if ($dejaDeSerAdministrador) {
                $this->exigirOtroAdministradorActivo($usuario, 'rol_id');
            }

            $usuario->update($datos);

            return $usuario;
        });
    }

    public function desactivar(Usuario $usuario, Usuario $actor): void
    {
        if ($usuario->is($actor)) {
            throw ValidationException::withMessages([
                'usuario' => 'No puedes desactivar tu propia cuenta.',
            ]);
        }

        DB::transaction(function () use ($usuario) {
            if ($usuario->tieneRol(Rol::ADMINISTRADOR)) {
                $this->exigirOtroAdministradorActivo($usuario, 'usuario');
            }

            $usuario->update(['activo' => false]);
        });
    }

    public function reactivar(Usuario $usuario): void
    {
        $usuario->update(['activo' => true]);
    }

    /**
     * El bloqueo de filas evita que dos administradores se desactiven entre sí
     * al mismo tiempo y el sistema quede sin ninguno.
     */
    private function exigirOtroAdministradorActivo(Usuario $usuario, string $campo): void
    {
        $administradores = Usuario::query()
            ->where('activo', true)
            ->whereHas('rol', fn ($rol) => $rol->where('nombre', Rol::ADMINISTRADOR))
            ->lockForUpdate()
            ->pluck('id');

        if ($administradores->reject(fn ($id) => $id === $usuario->id)->isEmpty()) {
            throw ValidationException::withMessages([
                $campo => 'El sistema necesita al menos un administrador activo.',
            ]);
        }
    }

    /**
     * Solo el rol Consulta conserva área; una contraseña vacía no cambia la actual.
     *
     * @param  array<string, mixed>  $datos
     * @return array<string, mixed>
     */
    private function normalizar(array $datos): array
    {
        if (Rol::find($datos['rol_id'])?->nombre !== Rol::CONSULTA) {
            $datos['area_id'] = null;
        }

        if (empty($datos['password'])) {
            unset($datos['password']);
        }

        return $datos;
    }
}
