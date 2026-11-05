<?php

namespace Tests\Feature;

use App\Models\Area;
use App\Models\Rol;
use App\Models\Usuario;
use Database\Seeders\DatabaseSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SeedersTest extends TestCase
{
    use RefreshDatabase;

    public function test_en_desarrollo_crea_un_usuario_por_rol(): void
    {
        config(['sigit.seed_password' => 'Clave1234']);

        $this->seed(DatabaseSeeder::class);

        $this->assertSame(3, Rol::count());
        $this->assertSame(3, Usuario::count());
        $this->assertNotNull(Usuario::where('correo', 'consulta@sigit.test')->value('area_id'));
    }

    public function test_sin_seed_password_no_crea_usuarios(): void
    {
        config(['sigit.seed_password' => null]);

        $this->seed(DatabaseSeeder::class);

        $this->assertSame(3, Rol::count());
        $this->assertSame(0, Usuario::count());
    }

    public function test_en_produccion_solo_crea_los_roles(): void
    {
        config(['sigit.seed_password' => 'Clave1234']);
        $this->app->detectEnvironment(fn () => 'production');

        $this->artisan('db:seed', ['--class' => DatabaseSeeder::class, '--force' => true])->assertSuccessful();

        $this->assertSame(3, Rol::count());
        $this->assertSame(0, Usuario::count());
        $this->assertSame(0, Area::count());
    }
}
