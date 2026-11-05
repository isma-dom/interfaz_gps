@php($rolConsulta = $roles->firstWhere('nombre', \App\Models\Rol::CONSULTA))
@php($esUnoMismo = $usuario->exists && $usuario->is(auth()->user()))

<div class="superficie formulario-usuario">
    <div class="mb-3">
        <label class="form-label" for="nombre">Nombre</label>
        <input class="form-control @error('nombre') is-invalid @enderror" id="nombre" name="nombre" type="text" maxlength="120"
               value="{{ old('nombre', $usuario->nombre) }}" autocomplete="off" required
               @error('nombre') aria-describedby="nombre-error" @enderror>
        @error('nombre') <div class="invalid-feedback" id="nombre-error">{{ $message }}</div> @enderror
    </div>

    <div class="mb-3">
        <label class="form-label" for="correo">Correo</label>
        <input class="form-control @error('correo') is-invalid @enderror" id="correo" name="correo" type="email" maxlength="150"
               value="{{ old('correo', $usuario->correo) }}" autocomplete="off" autocapitalize="none" spellcheck="false" required
               @error('correo') aria-describedby="correo-error" @enderror>
        @error('correo') <div class="invalid-feedback" id="correo-error">{{ $message }}</div> @enderror
    </div>

    <div class="mb-3">
        @if ($esUnoMismo)
            <span class="form-label d-block">Rol</span>
            <input type="hidden" id="rol_id" name="rol_id" value="{{ $usuario->rol_id }}">
            <p class="mb-0">{{ $usuario->rol->nombre }}. No puedes cambiar tu propio rol.</p>
        @else
            <label class="form-label" for="rol_id">Rol</label>
            <select class="form-select @error('rol_id') is-invalid @enderror" id="rol_id" name="rol_id" required
                    @error('rol_id') aria-describedby="rol-error" @enderror>
                <option value="">Elige un rol</option>
                @foreach ($roles as $rol)
                    <option value="{{ $rol->id }}" @selected((string) old('rol_id', $usuario->rol_id) === (string) $rol->id)>{{ $rol->nombre }}</option>
                @endforeach
            </select>
        @endif
        @error('rol_id') <div class="invalid-feedback d-block" id="rol-error">{{ $message }}</div> @enderror
    </div>

    <div class="mb-3" id="campo-area" data-rol-consulta="{{ $rolConsulta?->id }}">
        <label class="form-label" for="area_id">Área</label>
        <select class="form-select @error('area_id') is-invalid @enderror" id="area_id" name="area_id"
                aria-describedby="area-ayuda @error('area_id') area-error @enderror">
            <option value="">Elige un área</option>
            @foreach ($areas as $area)
                <option value="{{ $area->id }}" @selected((string) old('area_id', $usuario->area_id) === (string) $area->id)>{{ $area->nombre }}</option>
            @endforeach
        </select>
        <div class="form-text" id="area-ayuda">Solo para el rol Consulta: verá los equipos y resguardos de esta área.</div>
        @error('area_id') <div class="invalid-feedback" id="area-error">{{ $message }}</div> @enderror
    </div>

    <div class="mb-4">
        <label class="form-label" for="password">Contraseña</label>
        <div class="input-group has-validation">
            <input class="form-control @error('password') is-invalid @enderror" id="password" name="password" type="password"
                   autocomplete="new-password" @unless ($usuario->exists) required @endunless
                   aria-describedby="password-ayuda @error('password') password-error @enderror">
            <button class="btn btn-outline-secondary" type="button" data-ver-contrasena="password" aria-pressed="false" aria-label="Mostrar contraseña">
                <i class="bi bi-eye" aria-hidden="true"></i>
            </button>
            @error('password') <div class="invalid-feedback" id="password-error">{{ $message }}</div> @enderror
        </div>
        <div class="form-text" id="password-ayuda">
            Mínimo 8 caracteres, con letras y números.
            @if ($usuario->exists) Déjala vacía para conservar la actual. @endif
        </div>
    </div>

    <div class="d-flex flex-wrap gap-2">
        <button class="btn btn-primary" type="submit">{{ $textoBoton }}</button>
        <a class="btn btn-outline-secondary" href="{{ route('usuarios.index') }}">Cancelar</a>
    </div>
</div>
