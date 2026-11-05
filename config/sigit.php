<?php

return [

    /*
    | Bloqueo temporal de cuentas (C-01): 5 intentos fallidos con la misma
    | cuenta. Los documentos solo dicen «temporal»; 15 minutos es un supuesto
    | por confirmar con el Product Owner.
    */
    'bloqueo' => [
        'intentos' => 5,
        'minutos' => (int) env('SIGIT_BLOQUEO_MINUTOS', 15),
    ],

    // Minutos antes de que expire la sesión en que se muestra el aviso.
    'aviso_sesion_minutos' => 2,

    // Contraseña de los usuarios de prueba que crean los seeders en desarrollo.
    'seed_password' => env('SEED_PASSWORD'),

];
