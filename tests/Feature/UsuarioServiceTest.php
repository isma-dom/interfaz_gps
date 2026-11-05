<?php

namespace Tests\Feature;

use App\Models\Rol;
use App\Models\Usuario;
use App\Services\UsuarioService;
use Database\Seeders\RolSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Validation\ValidationException;
use Tests\TestCase;

/**
 * Por HTTP estas reglas no se alcanzan: quien actúa ya es un administrador activo
 * que no puede quitarse el rol. Protegen del caso de dos administradores simultáneos.
 */
class UsuarioServiceTest extends TestCase
{
    use RefreshDatabase;

    public function test_no_se_desactiva_al_ultimo_administrador_activo(): void
    {
        $ultimo = Usuario::factory()->administrador()->create();
        $actor = Usuario::factory()->administrador()->inactivo()->create();

        try {
            app(UsuarioService::class)->desactivar($ultimo, $actor);
            $this->fail('Debió rechazar la desactivación.');
        } catch (ValidationException $e) {
            $this->assertSame('El sistema necesita al menos un administrador activo.', $e->errors()['usuario'][0]);
        }

        $this->assertTrue($ultimo->fresh()->activo);
    }

    public function test_no_se_quita_el_rol_al_ultimo_administrador_activo(): void
    {
        $this->seed(RolSeeder::class);
        $ultimo = Usuario::factory()->administrador()->create();
        $actor = Usuario::factory()->administrador()->inactivo()->create();

        $this->expectException(ValidationException::class);

        app(UsuarioService::class)->actualizar($ultimo, [
            'nombre' => $ultimo->nombre,
            'correo' => $ultimo->correo,
            'rol_id' => Rol::where('nombre', Rol::EJECUTIVO)->value('id'),
            'area_id' => null,
            'password' => null,
        ], $actor);
    }

    public function test_desactivar_a_quien_no_es_administrador_no_exige_otro_administrador(): void
    {
        $usuario = Usuario::factory()->ejecutivo()->create();
        $actor = Usuario::factory()->administrador()->create();

        app(UsuarioService::class)->desactivar($usuario, $actor);

        $this->assertFalse($usuario->fresh()->activo);
    }
}
