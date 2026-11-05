<?php

namespace App\Models;

use Database\Factories\UsuarioFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Foundation\Auth\User as Authenticatable;

class Usuario extends Authenticatable
{
    /** @use HasFactory<UsuarioFactory> */
    use HasFactory;

    protected $table = 'usuarios';

    protected $fillable = ['rol_id', 'area_id', 'nombre', 'correo', 'password', 'activo'];

    protected $hidden = ['password'];

    protected function casts(): array
    {
        return [
            'password' => 'hashed',
            'activo' => 'boolean',
        ];
    }

    /** @return BelongsTo<Rol, $this> */
    public function rol(): BelongsTo
    {
        return $this->belongsTo(Rol::class, 'rol_id');
    }

    /** @return BelongsTo<Area, $this> */
    public function area(): BelongsTo
    {
        return $this->belongsTo(Area::class, 'area_id');
    }

    public function tieneRol(string ...$roles): bool
    {
        return in_array($this->rol?->nombre, $roles, true);
    }

    /** Página a la que llega el usuario al iniciar sesión. */
    public function rutaInicio(): string
    {
        return $this->tieneRol(Rol::CONSULTA) ? route('equipos') : route('tablero');
    }

    /** SIGIT no ofrece «Recuérdame»; la tabla no tiene remember_token. */
    public function getRememberTokenName(): string
    {
        return '';
    }
}
