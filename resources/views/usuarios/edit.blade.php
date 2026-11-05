@extends('layouts.app')

@section('titulo', 'Editar usuario')

@section('contenido')
    <form method="POST" action="{{ route('usuarios.update', $usuario) }}">
        @csrf
        @method('PUT')
        @include('usuarios._formulario', ['textoBoton' => 'Guardar cambios'])
    </form>
@endsection
