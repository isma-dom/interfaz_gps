<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call(RolSeeder::class);

        // Los usuarios de prueba nunca se crean en producción.
        if (app()->environment(['local', 'testing'])) {
            $this->call(DatosDePruebaSeeder::class);
        }
    }
}
