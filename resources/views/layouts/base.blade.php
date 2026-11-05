<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <meta name="robots" content="noindex">
    <title>@yield('titulo') | SIGIT</title>
    @vite(['resources/scss/app.scss', 'resources/js/app.js'])
</head>
<body @yield('atributos-body')>
    @yield('cuerpo')
</body>
</html>
