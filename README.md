# SIGIT

Sistema de Gestión de Inventario Tecnológico de la Subdirección de Administración de la Biblioteca Benito Juárez.

Esta rama contiene el módulo de autenticación y roles (RF-01, SS-01): pantallas P-01 y P-11, menú por rol y control de acceso. Stack: PHP 8.3, Laravel 12, MySQL 8.4, Blade y Bootstrap 5.3.

## Levantar el proyecto con Sail

Requiere Docker. Es el entorno de desarrollo oficial del proyecto.

```bash
cp .env.example .env
```

Define en `.env` el valor de `SEED_PASSWORD` (ver [Usuarios de prueba](#usuarios-de-prueba)) y luego ejecuta:

```bash
docker run --rm -u "$(id -u):$(id -g)" -v "$(pwd):/var/www/html" -w /var/www/html laravelsail/php83-composer:latest composer install --ignore-platform-reqs
```

```bash
./vendor/bin/sail up -d
```

```bash
./vendor/bin/sail artisan key:generate
```

```bash
./vendor/bin/sail artisan migrate:fresh --seed
```

```bash
./vendor/bin/sail npm ci
```

```bash
./vendor/bin/sail npm run build
```

La aplicación queda en http://localhost.

## Usuarios de prueba

`DatosDePruebaSeeder` crea tres áreas de ejemplo y un usuario por rol. Solo se ejecuta en los entornos `local` y `testing`, nunca en producción.

| Rol | Correo |
|---|---|
| Administrador | admin@sigit.test |
| Ejecutivo | ejecutivo@sigit.test |
| Consulta | consulta@sigit.test |

Los tres usan la contraseña de la variable `SEED_PASSWORD` de tu `.env`. Elige una con al menos 8 caracteres, letras y números. Si la dejas vacía, no se crean usuarios y el seeder lo avisa.

## Pruebas y calidad

```bash
./vendor/bin/sail artisan test
```

```bash
./vendor/bin/sail pint --test
```

```bash
./vendor/bin/sail bin phpstan analyse
```

Sin Docker, las pruebas también corren con PHP local y SQLite en memoria:

```bash
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
```

## Configuración del módulo

| Variable | Por omisión | Uso |
|---|---|---|
| `SESSION_LIFETIME` | 30 | Minutos de inactividad antes de que expire la sesión. |
| `SIGIT_BLOQUEO_MINUTOS` | 15 | Duración del bloqueo tras 5 intentos fallidos con la misma cuenta. |
| `SESSION_SECURE_COOKIE` | false | Usa `true` en staging y producción, con HTTPS. |
| `SEED_PASSWORD` | vacío | Contraseña de los usuarios de prueba. |

El menú y los permisos por pantalla se definen en `config/menu.php`; `routes/web.php` aplica esos roles a cada ruta.

## Decisiones por confirmar con el Product Owner

Los documentos del proyecto no definen estos puntos:

- Duración del bloqueo temporal: 15 minutos.
- Política de contraseñas: mínimo 8 caracteres, con letras y números.
- Página de inicio: Administrador y Ejecutivo llegan a `/tablero`; Consulta, a `/equipos`.
- Rutas web de las pantallas; el diseño solo define las de la API.
- Aviso de sesión 2 minutos antes de expirar, con el botón «Seguir conectado».
- Un administrador no puede desactivarse ni quitarse el rol, y el sistema no puede quedarse sin administradores activos.

## Pendiente fuera de este módulo

- API con Sanctum (`/api/v1/auth/login`): el evento PI-01 la exige, y `VerificarRol` ya responde JSON 403 para reutilizarlo.
- Logotipo oficial: sustituir el texto de `resources/views/partials/marca.blade.php` y de `resources/views/auth/login.blade.php`.
- Consulta de bitácora y `BitacoraObserver` (Sprint 4), CRUD de catálogos (P-10) y el resto de las pantallas, que hoy muestran «todavía no está disponible».
