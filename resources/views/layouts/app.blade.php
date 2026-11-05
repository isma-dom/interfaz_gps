@extends('layouts.base')

@section('atributos-body')
data-sesion-minutos="{{ config('session.lifetime') }}"
data-sesion-aviso-minutos="{{ config('sigit.aviso_sesion_minutos') }}"
data-sesion-url-mantener="{{ route('sesion.mantener') }}"
data-sesion-url-expirada="{{ route('login', ['expirada' => 1]) }}"
@endsection

@section('cuerpo')
    <a class="saltar-contenido" href="#contenido">Saltar al contenido</a>

    <div class="app-marco">
        <div class="app-lateral">
            <div class="offcanvas-lg offcanvas-start h-100" id="menu-lateral" tabindex="-1" aria-label="Menú">
                <div class="d-flex align-items-start justify-content-between">
                    @include('partials.marca')
                    <button type="button" class="btn-close m-2 d-lg-none" data-bs-dismiss="offcanvas" data-bs-target="#menu-lateral" aria-label="Cerrar menú"></button>
                </div>
                @include('partials.menu')
            </div>
        </div>

        <div class="app-cuerpo">
            <header class="app-encabezado">
                <button class="btn btn-outline-primary text-nowrap d-lg-none" type="button" data-bs-toggle="offcanvas" data-bs-target="#menu-lateral" aria-controls="menu-lateral">
                    <i class="bi bi-list" aria-hidden="true"></i> Menú
                </button>
                <div class="app-encabezado__usuario">
                    <span class="app-encabezado__nombre">{{ auth()->user()->nombre }}</span>
                    <span class="app-encabezado__rol">{{ auth()->user()->rol->nombre }}</span>
                </div>
                <form method="POST" action="{{ route('logout') }}">
                    @csrf
                    <button type="submit" class="btn btn-outline-secondary text-nowrap" aria-label="Cerrar sesión">
                        <i class="bi bi-box-arrow-right d-sm-none" aria-hidden="true"></i>
                        <span class="d-none d-sm-inline">Cerrar sesión</span>
                    </button>
                </form>
            </header>

            <main class="app-principal" id="contenido" tabindex="-1">
                <div class="titulo-pagina">
                    <h1>@yield('titulo')</h1>
                    @yield('acciones')
                </div>

                <div aria-live="polite">
                    @if (session('exito'))
                        <div class="aviso aviso--exito" role="status">
                            <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                            <span>{{ session('exito') }}</span>
                        </div>
                    @endif
                </div>

                @error('usuario')
                    <div class="aviso aviso--error" role="alert">
                        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                        <span>{{ $message }}</span>
                    </div>
                @enderror

                @yield('contenido')
            </main>
        </div>
    </div>

    @include('partials.aviso-sesion')
@endsection
