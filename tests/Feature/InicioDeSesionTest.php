<?php

namespace Tests\Feature;

use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\DataProvider;
use Tests\TestCase;

class InicioDeSesionTest extends TestCase
{
    use RefreshDatabase;

    private const MENU = ['Tablero', 'Equipos', 'Escaneo de QR', 'Resguardos', 'Importación y cuarentena', 'Reportes', 'Catálogos', 'Usuarios', 'Bitácora'];

    public static function menuPorRol(): array
    {
        return [
            'Administrador' => ['administrador', '/tablero', self::MENU],
            'Ejecutivo' => ['ejecutivo', '/tablero', ['Tablero', 'Equipos', 'Escaneo de QR', 'Resguardos', 'Reportes']],
            'Consulta' => ['consulta', '/equipos', ['Equipos', 'Escaneo de QR', 'Resguardos', 'Reportes']],
        ];
    }

    /** CP-01: cada rol entra y ve solo el menú que le corresponde. */
    #[DataProvider('menuPorRol')]
    public function test_cada_rol_entra_y_ve_solo_su_menu(string $estado, string $destino, array $menu): void
    {
        $usuario = Usuario::factory()->{$estado}()->create();

        $respuesta = $this->post('/login', ['correo' => $usuario->correo, 'password' => 'Clave1234']);

        $respuesta->assertRedirect($destino);
        $this->assertAuthenticatedAs($usuario);

        $pagina = $this->get($destino)->assertOk();
        $html = $this->menuDe($pagina->getContent());

        foreach (self::MENU as $entrada) {
            in_array($entrada, $menu, true)
                ? $this->assertStringContainsString($entrada, $html)
                : $this->assertStringNotContainsString($entrada, $html);
        }
    }

    public function test_correo_con_mayusculas_y_espacios_inicia_sesion(): void
    {
        Usuario::factory()->create(['correo' => 'ana@sigit.test']);

        $this->post('/login', ['correo' => '  ANA@sigit.test ', 'password' => 'Clave1234'])->assertRedirect();

        $this->assertAuthenticated();
    }

    public function test_credenciales_incorrectas_muestran_mensaje_generico(): void
    {
        $usuario = Usuario::factory()->create();

        $this->from('/login')->post('/login', ['correo' => $usuario->correo, 'password' => 'otra'])
            ->assertRedirect('/login')
            ->assertSessionHasErrors(['correo' => 'Correo o contraseña incorrectos.']);

        $this->assertGuest();
    }

    public function test_correo_inexistente_e_inactivo_dan_el_mismo_mensaje(): void
    {
        $inactivo = Usuario::factory()->inactivo()->create();

        $this->post('/login', ['correo' => 'nadie@sigit.test', 'password' => 'Clave1234'])
            ->assertSessionHasErrors(['correo' => 'Correo o contraseña incorrectos.']);

        $this->post('/login', ['correo' => $inactivo->correo, 'password' => 'Clave1234'])
            ->assertSessionHasErrors(['correo' => 'Correo o contraseña incorrectos.']);

        $this->assertGuest();
    }

    public function test_campos_vacios_muestran_errores_en_espanol(): void
    {
        $this->post('/login', ['correo' => '', 'password' => ''])
            ->assertSessionHasErrors(['correo' => 'Escribe el correo.', 'password' => 'Escribe la contraseña.']);
    }

    public function test_ruta_protegida_sin_sesion_vuelve_a_esa_ruta_al_entrar(): void
    {
        $administrador = Usuario::factory()->administrador()->create();

        $this->get('/usuarios')->assertRedirect('/login');

        $this->post('/login', ['correo' => $administrador->correo, 'password' => 'Clave1234'])
            ->assertRedirect('/usuarios');
    }

    public function test_usuario_con_sesion_que_abre_login_va_a_su_inicio(): void
    {
        $this->actingAs(Usuario::factory()->consulta()->create())->get('/login')->assertRedirect('/equipos');
        $this->actingAs(Usuario::factory()->ejecutivo()->create())->get('/login')->assertRedirect('/tablero');
    }

    public function test_cerrar_sesion_termina_la_sesion_y_exige_post(): void
    {
        $usuario = Usuario::factory()->create();

        $this->actingAs($usuario)->get('/logout')->assertMethodNotAllowed();
        $this->assertAuthenticated();

        $this->post('/logout')->assertRedirect('/login');
        $this->assertGuest();
    }

    public function test_no_existe_opcion_recuerdame_ni_recuperacion_de_contrasena(): void
    {
        $this->get('/login')->assertOk()
            ->assertSee('Inicia sesión')
            ->assertDontSee('Recuérdame')
            ->assertDontSee('Olvidaste');

        $this->get('/forgot-password')->assertNotFound();
        $this->get('/register')->assertNotFound();
    }

    private function menuDe(string $html): string
    {
        preg_match('/<nav aria-label="Menú principal">.*?<\/nav>/s', $html, $coincidencia);

        return $coincidencia[0];
    }
}
