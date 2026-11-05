<?php

namespace Tests\Feature;

use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/** CP-03: cinco intentos fallidos con la misma cuenta la bloquean temporalmente. */
class BloqueoDeCuentaTest extends TestCase
{
    use RefreshDatabase;

    private function fallar(string $correo, int $veces = 1, string $ip = '10.0.0.1'): void
    {
        for ($i = 0; $i < $veces; $i++) {
            $this->withServerVariables(['REMOTE_ADDR' => $ip])
                ->post('/login', ['correo' => $correo, 'password' => 'incorrecta']);
        }
    }

    public function test_cinco_intentos_fallidos_bloquean_la_cuenta(): void
    {
        $usuario = Usuario::factory()->create();

        $this->fallar($usuario->correo, 4);
        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'incorrecta'])
            ->assertSessionHasErrors(['correo' => 'Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.']);

        // Ni la contraseña correcta entra durante el bloqueo.
        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])
            ->assertSessionHasErrors('correo');
        $this->assertGuest();
    }

    public function test_cuatro_intentos_fallidos_no_bloquean(): void
    {
        $usuario = Usuario::factory()->create();

        $this->fallar($usuario->correo, 4);

        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])->assertRedirect('/tablero');
        $this->assertAuthenticatedAs($usuario);
    }

    public function test_el_bloqueo_alcanza_a_la_cuenta_desde_otra_ip(): void
    {
        $usuario = Usuario::factory()->create();

        $this->fallar($usuario->correo, 3, '10.0.0.1');
        $this->fallar($usuario->correo, 2, '10.0.0.2');

        $this->withServerVariables(['REMOTE_ADDR' => '10.0.0.3'])
            ->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])
            ->assertSessionHasErrors('correo');
        $this->assertGuest();
    }

    public function test_cambiar_mayusculas_del_correo_no_evita_el_bloqueo(): void
    {
        $usuario = Usuario::factory()->create(['correo' => 'ana@sigit.test']);

        $this->fallar('ana@sigit.test', 3);
        $this->fallar('ANA@sigit.test', 2);

        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])->assertSessionHasErrors('correo');
        $this->assertGuest();
    }

    public function test_el_bloqueo_termina_al_vencer_el_plazo(): void
    {
        $usuario = Usuario::factory()->create();

        $this->fallar($usuario->correo, 5);

        $this->travel(14)->minutes();
        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])->assertSessionHasErrors('correo');
        $this->assertGuest();

        $this->travel(61)->seconds();
        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])->assertRedirect('/tablero');
        $this->assertAuthenticatedAs($usuario);
    }

    public function test_el_plazo_cuenta_desde_el_ultimo_intento(): void
    {
        $usuario = Usuario::factory()->create();

        $this->fallar($usuario->correo, 4);
        $this->travel(14)->minutes();
        $this->fallar($usuario->correo);
        $this->travel(14)->minutes();

        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])->assertSessionHasErrors('correo');
        $this->assertGuest();
    }

    public function test_entrar_reinicia_el_conteo(): void
    {
        $usuario = Usuario::factory()->create();

        $this->fallar($usuario->correo, 4);
        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234']);
        $this->post('/logout');

        $this->fallar($usuario->correo, 4);
        $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234'])->assertRedirect('/tablero');
    }

    public function test_el_bloqueo_de_una_cuenta_no_afecta_a_otra(): void
    {
        $bloqueado = Usuario::factory()->create();
        $otro = Usuario::factory()->create();

        $this->fallar($bloqueado->correo, 5);

        $this->post('/login', ['correo' => $otro->correo, 'password' => 'Clave1234'])->assertRedirect('/tablero');
    }

    public function test_un_correo_inexistente_se_bloquea_igual_para_no_revelar_cuentas(): void
    {
        $this->fallar('nadie@sigit.test', 4);

        $this->post('/login', ['correo' => 'nadie@sigit.test', 'password' => 'incorrecta'])
            ->assertSessionHasErrors(['correo' => 'Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.']);
    }
}
