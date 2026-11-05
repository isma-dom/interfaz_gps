@extends('layouts.base')

@section('cuerpo')
    <main class="pagina-error">
        <div class="pagina-error__contenido">
            <div class="titulo-pagina">
                <h1>@yield('titulo')</h1>
            </div>
            <p>@yield('mensaje')</p>
            <a class="btn btn-primary" href="{{ url('/') }}">Ir a la página de inicio</a>
        </div>
    </main>
@endsection
