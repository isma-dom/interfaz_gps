@extends('layouts.app')

@section('titulo', 'Usuarios')

@section('acciones')
    <a class="btn btn-primary" href="{{ route('usuarios.create') }}">
        <i class="bi bi-person-plus" aria-hidden="true"></i> Agregar usuario
    </a>
@endsection

@section('contenido')
    <form class="filtros" method="GET" action="{{ route('usuarios.index') }}" role="search">
        <div class="filtros__busqueda">
            <label class="form-label" for="q">Buscar por nombre o correo</label>
            <input class="form-control" id="q" name="q" type="search" value="{{ $busqueda }}">
        </div>
        <div class="filtros__rol">
            <label class="form-label" for="rol">Rol</label>
            <select class="form-select" id="rol" name="rol">
                <option value="">Todos</option>
                @foreach ($roles as $rol)
                    <option value="{{ $rol->id }}" @selected($rolId === $rol->id)>{{ $rol->nombre }}</option>
                @endforeach
            </select>
        </div>
        <button class="btn btn-outline-primary" type="submit">Buscar</button>
        @if ($busqueda !== '' || $rolId)
            <a class="btn btn-link" href="{{ route('usuarios.index') }}">Quitar filtros</a>
        @endif
    </form>

    @if ($usuarios->isEmpty())
        <div class="superficie">
            <p class="mb-0">Ningún usuario coincide con la búsqueda. Prueba con otro nombre o quita los filtros.</p>
        </div>
    @else
        <div class="tabla-marco">
            <table class="table tabla-usuarios">
                <caption class="visually-hidden">Usuarios del sistema</caption>
                <thead>
                    <tr>
                        <th scope="col">Nombre</th>
                        <th scope="col">Correo</th>
                        <th scope="col">Rol</th>
                        <th scope="col">Área</th>
                        <th scope="col">Estado</th>
                        <th scope="col" class="text-end">Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    @foreach ($usuarios as $usuario)
                        <tr @class(['tabla-usuarios__inactivo' => ! $usuario->activo])>
                            <td class="tabla-usuarios__nombre">
                                {{ $usuario->nombre }}
                                @if ($usuario->is(auth()->user()))
                                    <span class="etiqueta-tu">Tú</span>
                                @endif
                            </td>
                            <td data-etiqueta="Correo">{{ $usuario->correo }}</td>
                            <td data-etiqueta="Rol">{{ $usuario->rol->nombre }}</td>
                            <td data-etiqueta="Área">{{ $usuario->area?->nombre ?? 'Sin área' }}</td>
                            <td data-etiqueta="Estado">
                                @if ($usuario->activo)
                                    <span class="estado-usuario estado-usuario--activo"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Activo</span>
                                @else
                                    <span class="estado-usuario estado-usuario--inactivo"><i class="bi bi-slash-circle" aria-hidden="true"></i> Inactivo</span>
                                @endif
                            </td>
                            <td class="tabla-usuarios__celda-acciones">
                                <div class="tabla-usuarios__acciones">
                                    <a class="btn btn-outline-primary btn-accion" href="{{ route('usuarios.edit', $usuario) }}" aria-label="Editar a {{ $usuario->nombre }}">Editar</a>
                                    @if ($usuario->activo)
                                        @unless ($usuario->is(auth()->user()))
                                            <button class="btn btn-outline-danger" type="button"
                                                    data-bs-toggle="modal" data-bs-target="#modal-desactivar"
                                                    data-url="{{ route('usuarios.desactivar', $usuario) }}"
                                                    data-nombre="{{ $usuario->nombre }}"
                                                    aria-label="Desactivar a {{ $usuario->nombre }}">Desactivar</button>
                                        @endunless
                                    @else
                                        <form method="POST" action="{{ route('usuarios.reactivar', $usuario) }}">
                                            @csrf
                                            @method('PATCH')
                                            <button class="btn btn-outline-primary" type="submit" aria-label="Reactivar a {{ $usuario->nombre }}">Reactivar</button>
                                        </form>
                                    @endif
                                </div>
                            </td>
                        </tr>
                    @endforeach
                </tbody>
            </table>
        </div>

        <div class="mt-3">
            {{ $usuarios->links() }}
        </div>
    @endif

    <div class="modal fade" id="modal-desactivar" tabindex="-1" aria-labelledby="modal-desactivar-titulo" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" method="POST">
                @csrf
                @method('PATCH')
                <div class="modal-header">
                    <h2 class="modal-title fs-5" id="modal-desactivar-titulo">¿Desactivar a <span data-nombre></span>?</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                </div>
                <div class="modal-body">
                    Ya no podrá iniciar sesión y perderá el acceso de inmediato. Su historial se conserva y puedes reactivarlo cuando quieras.
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="submit" class="btn btn-danger">Desactivar</button>
                </div>
            </form>
        </div>
    </div>
@endsection
