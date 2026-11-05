<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\View\View;

/** Pantallas de otros incrementos; sus rutas ya tienen el permiso por rol real. */
class EnConstruccionController extends Controller
{
    public function __invoke(Request $request): View
    {
        $entrada = collect(config('menu'))->firstWhere('ruta', $request->route()->getName());

        return view('en-construccion', ['titulo' => $entrada['etiqueta']]);
    }
}
