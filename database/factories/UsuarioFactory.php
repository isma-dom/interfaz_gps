<?php

namespace Database\Factories;

use App\Models\Area;
use App\Models\Rol;
use App\Models\Usuario;
use Illuminate\Database\Eloquent\Factories\Factory;

/** @extends Factory<Usuario> */
class UsuarioFactory extends Factory
{
    public function definition(): array
    {
        return [
            'rol_id' => fn () => Rol::firstOrCreate(['nombre' => Rol::EJECUTIVO])->id,
            'area_id' => null,
            'nombre' => fake()->name(),
            'correo' => fake()->unique()->userName().'@sigit.test',
            'password' => 'Clave1234',
            'activo' => true,
        ];
    }

    public function administrador(): static
    {
        return $this->state(['rol_id' => fn () => Rol::firstOrCreate(['nombre' => Rol::ADMINISTRADOR])->id]);
    }

    public function ejecutivo(): static
    {
        return $this->state(['rol_id' => fn () => Rol::firstOrCreate(['nombre' => Rol::EJECUTIVO])->id]);
    }

    public function consulta(): static
    {
        return $this->state([
            'rol_id' => fn () => Rol::firstOrCreate(['nombre' => Rol::CONSULTA])->id,
            'area_id' => fn () => Area::factory()->create()->id,
        ]);
    }

    public function inactivo(): static
    {
        return $this->state(['activo' => false]);
    }
}
