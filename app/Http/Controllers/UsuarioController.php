<?php

namespace App\Http\Controllers;

use App\Http\Requests\GuardarUsuarioRequest;
use App\Models\Area;
use App\Models\Rol;
use App\Models\Usuario;
use App\Services\UsuarioService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

/** C-02. Los usuarios no se borran: se desactivan. */
class UsuarioController extends Controller
{
    public function __construct(private readonly UsuarioService $servicio) {}

    public function index(Request $request): View
    {
        $busqueda = trim((string) $request->query('q'));
        $rolId = $request->integer('rol');

        $usuarios = Usuario::query()
            ->with(['rol', 'area'])
            ->when($busqueda !== '', function ($consulta) use ($busqueda) {
                $texto = '%'.addcslashes($busqueda, '%_\\').'%';
                $consulta->where(fn ($q) => $q->where('nombre', 'like', $texto)->orWhere('correo', 'like', $texto));
            })
            ->when($rolId, fn ($consulta) => $consulta->where('rol_id', $rolId))
            ->orderByDesc('activo')
            ->orderBy('nombre')
            ->paginate(15)
            ->withQueryString();

        return view('usuarios.index', [
            'usuarios' => $usuarios,
            'roles' => Rol::orderBy('nombre')->get(),
            'busqueda' => $busqueda,
            'rolId' => $rolId,
        ]);
    }

    public function create(): View
    {
        return view('usuarios.create', $this->catalogos() + ['usuario' => new Usuario]);
    }

    public function store(GuardarUsuarioRequest $request): RedirectResponse
    {
        $usuario = $this->servicio->crear($request->validated());

        return redirect()->route('usuarios.index')->with('exito', "Usuario {$usuario->nombre} agregado.");
    }

    public function edit(Usuario $usuario): View
    {
        return view('usuarios.edit', $this->catalogos() + ['usuario' => $usuario]);
    }

    public function update(GuardarUsuarioRequest $request, Usuario $usuario): RedirectResponse
    {
        $this->servicio->actualizar($usuario, $request->validated(), $request->user());

        return redirect()->route('usuarios.index')->with('exito', "Cambios de {$usuario->nombre} guardados.");
    }

    public function desactivar(Request $request, Usuario $usuario): RedirectResponse
    {
        $this->servicio->desactivar($usuario, $request->user());

        return redirect()->route('usuarios.index')->with('exito', "Usuario {$usuario->nombre} desactivado.");
    }

    public function reactivar(Usuario $usuario): RedirectResponse
    {
        $this->servicio->reactivar($usuario);

        return redirect()->route('usuarios.index')->with('exito', "Usuario {$usuario->nombre} reactivado.");
    }

    /** @return array<string, mixed> */
    private function catalogos(): array
    {
        return [
            'roles' => Rol::orderBy('nombre')->get(),
            'areas' => Area::orderBy('nombre')->get(),
        ];
    }
}
