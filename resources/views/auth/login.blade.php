@extends('layouts.base')

@section('titulo', 'Inicia sesión')

@section('cuerpo')
    <main class="pagina-ficha">
        <div @class(['ficha', 'ficha--entra' => ! $errors->any() && ! session('aviso') && ! $sesionExpirada])>
            <div class="ficha__encabezado">
                {{-- Sustituir por el logotipo oficial cuando el cliente lo entregue. --}}
                <span class="ficha__sigla">SIGIT</span>
                <span class="ficha__institucion">Biblioteca Benito Juárez<br>Inventario tecnológico</span>
            </div>

            <h1>Inicia sesión</h1>
            <p class="ficha__indicacion">Usa el correo y la contraseña que te dio Soporte Técnico.</p>

            <div aria-live="polite">
                @if (session('aviso') || $sesionExpirada)
                    <p class="ficha__aviso" role="status">{{ session('aviso') ?? 'Tu sesión expiró por inactividad.' }}</p>
                @endif
            </div>

            <form method="POST" action="{{ url('login') }}">
                @csrf

                <div class="ficha__campo">
                    <label class="form-label" for="correo">Correo</label>
                    <input class="form-control @error('correo') is-invalid @enderror" id="correo" name="correo" type="email"
                           value="{{ old('correo') }}" autocomplete="username" inputmode="email" autocapitalize="none" spellcheck="false"
                           required @if (! $errors->has('password')) autofocus @endif
                           @error('correo') aria-invalid="true" aria-describedby="correo-error" @enderror>
                    @error('correo')
                        <p class="ficha__error" id="correo-error" role="alert">{{ $message }}</p>
                    @enderror
                </div>

                <div class="ficha__campo">
                    <label class="form-label" for="password">Contraseña</label>
                    <div class="ficha__contrasena">
                        <input class="form-control @error('password') is-invalid @enderror" id="password" name="password" type="password"
                               autocomplete="current-password" required
                               @error('password') aria-invalid="true" aria-describedby="password-error" @enderror>
                        <button class="boton-ver" type="button" data-ver-contrasena="password" aria-pressed="false" aria-label="Mostrar contraseña">
                            <i class="bi bi-eye" aria-hidden="true"></i>
                        </button>
                    </div>
                    @error('password')
                        <p class="ficha__error" id="password-error" role="alert">{{ $message }}</p>
                    @enderror
                </div>

                <button class="btn btn-primary ficha__accion" type="submit">Entrar</button>
            </form>

            <p class="ficha__ayuda">¿No puedes entrar? Pide ayuda a Soporte Técnico.</p>
        </div>
    </main>
@endsection
