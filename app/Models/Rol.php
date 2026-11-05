<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Rol extends Model
{
    public const ADMINISTRADOR = 'Administrador';

    public const EJECUTIVO = 'Ejecutivo';

    public const CONSULTA = 'Consulta';

    protected $table = 'roles';

    public $timestamps = false;

    protected $fillable = ['nombre'];

    /** @return HasMany<Usuario, $this> */
    public function usuarios(): HasMany
    {
        return $this->hasMany(Usuario::class, 'rol_id');
    }
}
