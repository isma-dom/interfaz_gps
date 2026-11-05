<?php

namespace Tests\Feature;

use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Route;
use Tests\TestCase;

class SesionTest extends TestCase
{
    use RefreshDatabase;

    public function test_la_sesion_dura_30_minutos_sin_actividad(): void
    {
        $this->assertSame(30, config('session.lifetime'));
        $this->assertFalse(config('session.expire_on_close'));
    }

    public function test_el_layout_recibe_los_datos_del_aviso_de_expiracion(): void
    {
        $this->actingAs(Usuario::factory()->create())->get('/equipos')
            ->assertSee('data-sesion-minutos="30"', false)
            ->assertSee('data-sesion-aviso-minutos="2"', false)
            ->assertSee('Tu sesión está por terminar');
    }

    public function test_seguir_conectado_responde_sin_contenido(): void
    {
        $this->actingAs(Usuario::factory()->create())->postJson('/sesion/mantener')->assertNoContent();
    }

    public function test_seguir_conectado_sin_sesion_recibe_401(): void
    {
        $this->postJson('/sesion/mantener')->assertUnauthorized();
    }

    public function test_el_login_muestra_que_la_sesion_expiro(): void
    {
        $this->get('/login?expirada=1')->assertOk()->assertSee('Tu sesión expiró por inactividad.');
        $this->get('/login')->assertOk()->assertDontSee('Tu sesión expiró por inactividad.');
    }

    public function test_un_error_419_lleva_al_login_con_el_mensaje_de_sesion_expirada(): void
    {
        Route::middleware('web')->post('/prueba-419', fn () => abort(419));

        $this->post('/prueba-419')
            ->assertRedirect('/login')
            ->assertSessionHas('aviso', 'Tu sesión expiró por inactividad.');
    }

    public function test_un_error_419_en_json_conserva_el_codigo(): void
    {
        Route::middleware('web')->post('/prueba-419', fn () => abort(419));

        $this->postJson('/prueba-419')->assertStatus(419);
    }

    public function test_usuario_desactivado_con_sesion_abierta_pierde_el_acceso(): void
    {
        $usuario = Usuario::factory()->create();

        $this->actingAs($usuario)->get('/equipos')->assertOk();

        $usuario->update(['activo' => false]);

        $this->get('/equipos')
            ->assertRedirect('/login')
            ->assertSessionHas('aviso', 'Tu acceso fue desactivado. Pide ayuda a Soporte Técnico.');
        $this->assertGuest();
    }

    public function test_usuario_desactivado_en_json_recibe_401(): void
    {
        $usuario = Usuario::factory()->create();

        $this->actingAs($usuario);
        $usuario->update(['activo' => false]);

        $this->getJson('/equipos')->assertUnauthorized();
    }
}
