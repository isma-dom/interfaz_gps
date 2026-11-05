<?php

namespace Database\Factories;

use App\Models\Area;
use Illuminate\Database\Eloquent\Factories\Factory;

/** @extends Factory<Area> */
class AreaFactory extends Factory
{
    public function definition(): array
    {
        return [
            'nombre' => 'Área '.fake()->unique()->numerify('#####'),
            'edificio' => 'Edificio principal',
            'piso' => (string) fake()->numberBetween(1, 3),
        ];
    }
}
