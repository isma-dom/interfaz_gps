<nav aria-label="Menú principal">
    <ul class="app-menu">
        @foreach (config('menu') as $entrada)
            @if (auth()->user()->tieneRol(...$entrada['roles']))
                @php($actual = request()->routeIs($entrada['ruta']) || request()->is($entrada['url'].'/*'))
                <li>
                    <a href="{{ route($entrada['ruta']) }}" @if ($actual) aria-current="page" @endif>
                        <i class="bi bi-{{ $entrada['icono'] }}" aria-hidden="true"></i>
                        {{ $entrada['etiqueta'] }}
                    </a>
                </li>
            @endif
        @endforeach
    </ul>
</nav>
