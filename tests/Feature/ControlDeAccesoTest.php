<?php

namespace Tests\Feature;

use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\DataProvider;
use Tests\TestCase;

class ControlDeAccesoTest extends TestCase
{
    use RefreshDatabase;

    /** Matriz de AD 2.2.7. Se escribe aparte de config/menu.php para detectar diferencias. */
    private const MATRIZ = [
        '/tablero' => ['administrador' => true, 'ejecutivo' => true, 'consulta' => false],
        '/equipos' => ['administrador' => true, 'ejecutivo' => true, 'consulta' => true],
        '/escaneo' => ['administrador' => true, 'ejecutivo' => true, 'consulta' => true],
        '/resguardos' => ['administrador' => true, 'ejecutivo' => true, 'consulta' => true],
        '/importaciones' => ['administrador' => true, 'ejecutivo' => false, 'consulta' => false],
        '/reportes' => ['administrador' => true, 'ejecutivo' => true, 'consulta' => true],
        '/catalogos' => ['administrador' => true, 'ejecutivo' => false, 'consulta' => false],
        '/usuarios' => ['administrador' => true, 'ejecutivo' => false, 'consulta' => false],
        '/bitacora' => ['administrador' => true, 'ejecutivo' => false, 'consulta' => false],
    ];

    public static function rolesYRutas(): array
    {
        $casos = [];

        foreach (self::MATRIZ as $ruta => $roles) {
            foreach ($roles as $rol => $permitido) {
                $casos["$rol en $ruta"] = [$rol, $ruta, $permitido];
            }
        }

        return $casos;
    }

    public static function rutas(): array
    {
        return array_map(fn ($ruta) => [$ruta], array_combine(array_keys(self::MATRIZ), array_keys(self::MATRIZ)));
    }

    #[DataProvider('rolesYRutas')]
    public function test_cada_rol_accede_solo_a_lo_permitido(string $rol, string $ruta, bool $permitido): void
    {
        $this->actingAs(Usuario::factory()->{$rol}()->create())
            ->get($ruta)
            ->assertStatus($permitido ? 200 : 403);
    }

    #[DataProvider('rutas')]
    public function test_sin_sesion_redirige_al_login(string $ruta): void
    {
        $this->get($ruta)->assertRedirect('/login');
    }

    public function test_la_pagina_403_esta_en_espanol_y_lleva_al_inicio(): void
    {
        $this->actingAs(Usuario::factory()->consulta()->create())
            ->get('/usuarios')
            ->assertForbidden()
            ->assertSee('No tienes permiso para ver esta página')
            ->assertSee('Ir a la página de inicio');
    }

    public function test_verificar_rol_responde_json_si_la_peticion_espera_json(): void
    {
        $this->actingAs(Usuario::factory()->consulta()->create())
            ->getJson('/usuarios')
            ->assertForbidden()
            ->assertJson(['message' => 'No tienes permiso para realizar esta acción.']);
    }

    public function test_sin_sesion_una_peticion_json_recibe_401(): void
    {
        $this->getJson('/usuarios')->assertUnauthorized();
    }

    public function test_la_pagina_404_esta_en_espanol(): void
    {
        $this->get('/no-existe')->assertNotFound()->assertSee('No encontramos esta página');
    }

    public function test_las_paginas_con_sesion_no_se_guardan_en_cache(): void
    {
        $control = $this->actingAs(Usuario::factory()->create())->get('/equipos')->headers->get('Cache-Control');

        $this->assertStringContainsString('no-store', $control);
    }
}
