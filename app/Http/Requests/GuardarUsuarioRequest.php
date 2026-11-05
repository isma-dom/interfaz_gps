<?php

namespace App\Http\Requests;

use App\Models\Rol;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Support\Str;
use Illuminate\Validation\Rule;
use Illuminate\Validation\Rules\Password;

class GuardarUsuarioRequest extends FormRequest
{
    /** @return array<string, list<mixed>> */
    public function rules(): array
    {
        $usuario = $this->route('usuario');
        $esConsulta = fn () => Rol::find($this->input('rol_id'))?->nombre === Rol::CONSULTA;

        return [
            'nombre' => ['required', 'string', 'max:120'],
            'correo' => ['required', 'string', 'email', 'max:150', Rule::unique('usuarios', 'correo')->ignore($usuario)],
            'rol_id' => ['required', 'integer', Rule::exists('roles', 'id')],
            'area_id' => ['nullable', Rule::requiredIf($esConsulta), 'integer', Rule::exists('areas', 'id')],
            // Supuesto por confirmar: los documentos no definen la política de contraseñas.
            'password' => [$usuario ? 'nullable' : 'required', 'string', Password::min(8)->letters()->numbers()],
        ];
    }

    protected function prepareForValidation(): void
    {
        $this->merge(['correo' => Str::lower(trim((string) $this->input('correo')))]);
    }
}
