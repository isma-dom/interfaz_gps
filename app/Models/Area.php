<?php

namespace App\Models;

use Database\Factories\AreaFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Area extends Model
{
    /** @use HasFactory<AreaFactory> */
    use HasFactory;

    protected $table = 'areas';

    public $timestamps = false;

    protected $fillable = ['nombre', 'edificio', 'piso'];

    /** @return HasMany<Usuario, $this> */
    public function usuarios(): HasMany
    {
        return $this->hasMany(Usuario::class, 'area_id');
    }
}
