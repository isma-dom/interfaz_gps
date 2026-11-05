<?php

namespace Database\Seeders;

use App\Models\Area;
use App\Models\Rol;
use App\Models\Usuario;
use Illuminate\Database\Seeder;

/** Áreas de ejemplo y un usuario por rol. El catálogo real lo entrega el cliente. */
class DatosDePruebaSeeder extends Seeder
{
    public function run(): void
    {
        $sala = Area::firstOrCreate(['nombre' => 'Sala de consulta'], ['edificio' => 'Edificio principal', 'piso' => '1']);
        Area::firstOrCreate(['nombre' => 'Procesos técnicos'], ['edificio' => 'Edificio principal', 'piso' => '2']);
        Area::firstOrCreate(['nombre' => 'Soporte Técnico'], ['edificio' => 'Edificio principal', 'piso' => '1']);

        $contrasena = config('sigit.seed_password');

        if (empty($contrasena)) {
            $this->command->warn('SEED_PASSWORD está vacío: no se crearon usuarios de prueba.');

            return;
        }

        $usuarios = [
            ['admin@sigit.test', 'Administrador de prueba', Rol::ADMINISTRADOR, null],
            ['ejecutivo@sigit.test', 'Ejecutivo de prueba', Rol::EJECUTIVO, null],
            ['consulta@sigit.test', 'Consulta de prueba', Rol::CONSULTA, $sala->id],
        ];

        foreach ($usuarios as [$correo, $nombre, $rol, $areaId]) {
            Usuario::firstOrCreate(['correo' => $correo], [
                'nombre' => $nombre,
                'password' => $contrasena,
                'rol_id' => Rol::where('nombre', $rol)->value('id'),
                'area_id' => $areaId,
            ]);
        }
    }
}
