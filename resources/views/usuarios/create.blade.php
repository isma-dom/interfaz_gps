@extends('layouts.app')

@section('titulo', 'Agregar usuario')

@section('contenido')
    <form method="POST" action="{{ route('usuarios.store') }}">
        @csrf
        @include('usuarios._formulario', ['textoBoton' => 'Agregar usuario'])
    </form>
@endsection
