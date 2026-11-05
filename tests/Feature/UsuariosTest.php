<?php

namespace Tests\Feature;

use App\Models\Area;
use App\Models\Rol;
use App\Models\Usuario;
use Database\Seeders\RolSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class UsuariosTest extends TestCase
{
    use RefreshDatabase;

    private Usuario $administrador;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(RolSeeder::class);
        $this->administrador = Usuario::factory()->administrador()->create();
        $this->actingAs($this->administrador);
    }

    private function rol(string $nombre): int
    {
        return Rol::where('nombre', $nombre)->value('id');
    }

    /** @return array<string, mixed> */
    private function datos(array $cambios = []): array
    {
        return $cambios + [
            'nombre' => 'Ana Canul',
            'correo' => 'ana@sigit.test',
            'rol_id' => $this->rol(Rol::EJECUTIVO),
            'area_id' => '',
            'password' => 'Clave1234',
        ];
    }

    public function test_solo_el_administrador_gestiona_usuarios(): void
    {
        $objetivo = Usuario::factory()->create();

        foreach (['ejecutivo', 'consulta'] as $estado) {
            $this->actingAs(Usuario::factory()->{$estado}()->create());

            $this->get('/usuarios')->assertForbidden();
            $this->get('/usuarios/create')->assertForbidden();
            $this->post('/usuarios', $this->datos())->assertForbidden();
            $this->get("/usuarios/{$objetivo->id}/edit")->assertForbidden();
            $this->put("/usuarios/{$objetivo->id}", $this->datos())->assertForbidden();
            $this->patch("/usuarios/{$objetivo->id}/desactivar")->assertForbidden();
            $this->patch("/usuarios/{$objetivo->id}/reactivar")->assertForbidden();
        }

        $this->assertDatabaseMissing('usuarios', ['correo' => 'ana@sigit.test']);
        $this->assertTrue($objetivo->fresh()->activo);
    }

    public function test_agregar_usuario_guarda_la_contrasena_con_hash(): void
    {
        $this->post('/usuarios', $this->datos())
            ->assertRedirect('/usuarios')
            ->assertSessionHas('exito', 'Usuario Ana Canul agregado.');

        $usuario = Usuario::where('correo', 'ana@sigit.test')->firstOrFail();

        $this->assertNotSame('Clave1234', $usuario->password);
        $this->assertTrue(Hash::check('Clave1234', $usuario->password));
        $this->assertTrue($usuario->activo);
    }

    public function test_correo_duplicado_da_error(): void
    {
        Usuario::factory()->create(['correo' => 'ana@sigit.test']);

        $this->post('/usuarios', $this->datos(['correo' => 'ANA@sigit.test']))
            ->assertSessionHasErrors(['correo' => 'Ya existe un registro con el correo indicado.']);

        $this->assertSame(1, Usuario::where('correo', 'ana@sigit.test')->count());
    }

    public function test_area_obligatoria_solo_en_consulta(): void
    {
        $this->post('/usuarios', $this->datos(['rol_id' => $this->rol(Rol::CONSULTA)]))
            ->assertSessionHasErrors(['area_id' => 'Elige el área de este usuario.']);

        $area = Area::factory()->create();

        $this->post('/usuarios', $this->datos(['rol_id' => $this->rol(Rol::CONSULTA), 'area_id' => $area->id]))
            ->assertSessionHasNoErrors();

        $this->assertSame($area->id, Usuario::where('correo', 'ana@sigit.test')->value('area_id'));
    }

    public function test_area_se_guarda_nula_si_el_rol_no_es_consulta(): void
    {
        $area = Area::factory()->create();

        $this->post('/usuarios', $this->datos(['area_id' => $area->id]))->assertSessionHasNoErrors();

        $this->assertNull(Usuario::where('correo', 'ana@sigit.test')->value('area_id'));
    }

    public function test_cambiar_de_consulta_a_otro_rol_borra_el_area(): void
    {
        $usuario = Usuario::factory()->consulta()->create();

        $this->put("/usuarios/{$usuario->id}", $this->datos([
            'correo' => $usuario->correo,
            'rol_id' => $this->rol(Rol::EJECUTIVO),
            'area_id' => $usuario->area_id,
            'password' => '',
        ]))->assertSessionHasNoErrors();

        $this->assertNull($usuario->fresh()->area_id);
    }

    public function test_politica_de_contrasena(): void
    {
        $this->post('/usuarios', $this->datos(['password' => '']))->assertSessionHasErrors('password');
        $this->post('/usuarios', $this->datos(['password' => 'Corta1']))->assertSessionHasErrors('password');
        $this->post('/usuarios', $this->datos(['password' => 'SoloLetras']))->assertSessionHasErrors('password');
        $this->post('/usuarios', $this->datos(['password' => '12345678']))->assertSessionHasErrors('password');
    }

    public function test_contrasena_vacia_en_edicion_no_la_cambia(): void
    {
        $usuario = Usuario::factory()->create();
        $anterior = $usuario->password;

        $this->put("/usuarios/{$usuario->id}", $this->datos(['correo' => $usuario->correo, 'password' => '']))
            ->assertSessionHasNoErrors();

        $this->assertSame($anterior, $usuario->fresh()->password);
        $this->assertSame('Ana Canul', $usuario->fresh()->nombre);
    }

    public function test_contrasena_nueva_en_edicion_la_cambia(): void
    {
        $usuario = Usuario::factory()->create();

        $this->put("/usuarios/{$usuario->id}", $this->datos(['correo' => $usuario->correo, 'password' => 'Nueva1234']))
            ->assertSessionHasNoErrors();

        $this->assertTrue(Hash::check('Nueva1234', $usuario->fresh()->password));
    }

    public function test_editar_conserva_su_propio_correo(): void
    {
        $usuario = Usuario::factory()->create(['correo' => 'luis@sigit.test']);

        $this->put("/usuarios/{$usuario->id}", $this->datos(['correo' => 'luis@sigit.test', 'password' => '']))
            ->assertSessionHasNoErrors();
    }

    public function test_desactivar_y_reactivar(): void
    {
        $usuario = Usuario::factory()->create(['nombre' => 'Luis Pech']);

        $this->patch("/usuarios/{$usuario->id}/desactivar")
            ->assertRedirect('/usuarios')
            ->assertSessionHas('exito', 'Usuario Luis Pech desactivado.');
        $this->assertFalse($usuario->fresh()->activo);

        $this->patch("/usuarios/{$usuario->id}/reactivar")
            ->assertSessionHas('exito', 'Usuario Luis Pech reactivado.');
        $this->assertTrue($usuario->fresh()->activo);
    }

    public function test_no_existe_ruta_de_eliminacion(): void
    {
        $usuario = Usuario::factory()->create();

        $this->delete("/usuarios/{$usuario->id}")->assertMethodNotAllowed();
        $this->assertDatabaseHas('usuarios', ['id' => $usuario->id]);
    }

    public function test_no_puedes_desactivarte_a_ti_mismo(): void
    {
        $otro = Usuario::factory()->administrador()->create();

        $this->patch("/usuarios/{$this->administrador->id}/desactivar")
            ->assertSessionHasErrors(['usuario' => 'No puedes desactivar tu propia cuenta.']);

        $this->assertTrue($this->administrador->fresh()->activo);
        $this->assertTrue($otro->fresh()->activo);
    }

    public function test_no_puedes_quitarte_el_rol_de_administrador(): void
    {
        Usuario::factory()->administrador()->create();

        $this->put("/usuarios/{$this->administrador->id}", $this->datos([
            'correo' => $this->administrador->correo,
            'password' => '',
        ]))->assertSessionHasErrors(['rol_id' => 'No puedes quitarte el rol de Administrador.']);

        $this->assertTrue($this->administrador->fresh()->tieneRol(Rol::ADMINISTRADOR));
    }

    public function test_puedes_editar_tus_datos_conservando_tu_rol(): void
    {
        $this->put("/usuarios/{$this->administrador->id}", $this->datos([
            'correo' => $this->administrador->correo,
            'rol_id' => $this->administrador->rol_id,
            'password' => '',
        ]))->assertSessionHasNoErrors();

        $this->assertSame('Ana Canul', $this->administrador->fresh()->nombre);
    }

    public function test_un_administrador_puede_desactivar_a_otro_si_queda_uno(): void
    {
        $otro = Usuario::factory()->administrador()->create();

        $this->patch("/usuarios/{$otro->id}/desactivar")->assertSessionHasNoErrors();

        $this->assertFalse($otro->fresh()->activo);
    }

    public function test_la_lista_busca_por_nombre_o_correo_y_filtra_por_rol(): void
    {
        Usuario::factory()->ejecutivo()->create(['nombre' => 'Marta Euan', 'correo' => 'marta@sigit.test']);
        Usuario::factory()->consulta()->create(['nombre' => 'Pedro Dzul', 'correo' => 'pedro@sigit.test']);

        $this->get('/usuarios?q=marta')->assertSee('Marta Euan')->assertDontSee('Pedro Dzul');
        $this->get('/usuarios?q=pedro@')->assertSee('Pedro Dzul')->assertDontSee('Marta Euan');
        $this->get('/usuarios?rol='.$this->rol(Rol::CONSULTA))->assertSee('Pedro Dzul')->assertDontSee('Marta Euan');
        $this->get('/usuarios?q=zzz')->assertSee('Ningún usuario coincide con la búsqueda');
    }

    public function test_el_comodin_de_busqueda_se_trata_como_texto(): void
    {
        Usuario::factory()->create(['nombre' => 'Marta Euan']);

        $this->get('/usuarios?q=%25')->assertDontSee('Marta Euan');
    }

    public function test_la_lista_esta_paginada_y_conserva_los_filtros(): void
    {
        Usuario::factory()->ejecutivo()->count(20)->create(['nombre' => 'Persona']);

        $this->get('/usuarios?q=Persona')->assertOk()->assertSee('q=Persona&amp;page=2', false);
    }

    public function test_los_nombres_se_escapan_en_la_lista(): void
    {
        Usuario::factory()->create(['nombre' => '<script>alert(1)</script>']);

        $this->get('/usuarios')->assertOk()->assertDontSee('<script>alert(1)</script>', false);
    }

    public function test_tu_propia_fila_no_ofrece_desactivar(): void
    {
        $this->get('/usuarios')
            ->assertSee('Tú')
            ->assertDontSee(route('usuarios.desactivar', $this->administrador));
    }
}
