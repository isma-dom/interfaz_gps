# SIGIT

Sistema de Gestión de Inventario Tecnológico de la Subdirección de Administración de la Biblioteca Benito Juárez.

Esta versión contiene el módulo de autenticación y roles (RF-01, SS-01): pantallas P-01 y P-11, menú por rol y control de acceso.

| Capa | Tecnología |
|---|---|
| Lenguaje y framework | Java 21, Spring Boot 4.1 (Spring MVC, Spring Security, Spring Data JPA) |
| Interfaz | HTML con Thymeleaf, CSS y JavaScript sobre Bootstrap 5.3, servidos desde el WAR sin CDN |
| Base de datos | MySQL 8.4, con migraciones de Flyway |
| Servidor | Apache Tomcat 11 (`sigit.war`); en desarrollo, el Tomcat embebido de Spring Boot |

## Requisitos

- JDK 21.
- Docker, para el MySQL de desarrollo y para las pruebas.

Maven no hace falta: el proyecto trae Maven Wrapper (`./mvnw`).

## Levantar en desarrollo

```bash
export SIGIT_SEED_PASSWORD='<elige una contraseña con letras y números>'
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Spring Boot levanta solo el MySQL de `compose.yaml`, aplica las migraciones y abre la aplicación en http://localhost:8080.

## Alta de equipos (Tarea 2)

El Administrador registra equipos en Equipos → Registrar equipo (http://localhost:8080/equipos/nuevo). Se guardan en la tabla `equipos` de MySQL, creada por la migración `V2__inventario.sql` con los catálogos iniciales de tipos y marcas. Antes del primer alta tiene que existir al menos un área.

## Prototipo de equipos (Tarea 1)

Con el perfil `dev` existe un prototipo navegable de las pantallas P-03 (listado) y P-05 (registro y edición) de equipos, con 26 equipos de ejemplo en memoria: http://localhost:8080/prototipos/equipos. Registrar, editar y dar de baja es solo del Administrador; Consulta ve únicamente su área. Los equipos se pierden al reiniciar: guardarlos en MySQL es una tarea posterior.

## Usuarios de prueba

El perfil `dev` crea tres áreas de ejemplo y un usuario por rol. Fuera de ese perfil no se crean.

| Rol | Correo |
|---|---|
| Administrador | admin@sigit.test |
| Ejecutivo | ejecutivo@sigit.test |
| Consulta | consulta@sigit.test |

Los tres usan la contraseña de `SIGIT_SEED_PASSWORD`. Si la variable está vacía, no se crean usuarios y el arranque lo avisa.

## Pruebas

```bash
./mvnw test
```

Las pruebas corren contra MySQL 8.4 real con Testcontainers, así que Docker debe estar abierto. Cubren CP-01, CP-03, la matriz de rol por ruta, la sesión, la gestión de usuarios y, con un Tomcat embebido real, las páginas de error y las cabeceras de seguridad.

## Desplegar en Apache Tomcat 11

1. Genera el WAR (queda en `target/sigit.war`):

   ```bash
   ./mvnw package
   ```

2. Define las variables en `$CATALINA_HOME/bin/setenv.sh`:

   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export SIGIT_DB_URL='jdbc:mysql://localhost:3306/sigit'
   export SIGIT_DB_USUARIO='sigit'
   export SIGIT_DB_PASSWORD='<contraseña de MySQL>'
   ```

3. Copia `sigit.war` a `$CATALINA_HOME/webapps/`. Queda en `/sigit`; para servirlo en la raíz, cópialo como `ROOT.war`.

Con el perfil `prod`, la aplicación no arranca si falta alguna variable, y la cookie de sesión solo viaja por HTTPS. Si Nginx termina el HTTPS delante de Tomcat, configura `RemoteIpValve` en `server.xml` para que las redirecciones usen `https`.

## Configuración del módulo

| Propiedad o variable | Por omisión | Uso |
|---|---|---|
| `sigit.sesion.minutos` | 30 | Minutos de inactividad antes de que expire la sesión. |
| `sigit.sesion.aviso-minutos` | 2 | Minutos antes de expirar en que aparece el aviso «Seguir conectado». |
| `SIGIT_BLOQUEO_MINUTOS` | 15 | Duración del bloqueo tras 5 intentos fallidos con la misma cuenta. |
| `SIGIT_SEED_PASSWORD` | vacío | Contraseña de los usuarios de prueba del perfil `dev`. |

La duración de la sesión y la cookie (`HttpOnly`, `SameSite=Lax`) se aplican en `ConfiguracionGeneral`, porque las propiedades `server.servlet.session.*` de Spring Boot no llegan a un Tomcat externo.

## Dónde está cada componente del diseño

| Diseño (SIGIT-DMS-A3.2-AD) | Implementación |
|---|---|
| C-01 AuthController | `web/AuthController`, `seguridad/ProveedorDeAutenticacion`, `seguridad/BloqueoDeAcceso` |
| C-02 UsuarioController | `usuarios/UsuarioController`, `usuarios/UsuarioService` |
| C-03 Usuario, Rol | `seguridad/Usuario`, `seguridad/Rol` |
| C-04 VerificarRol | `config/SeguridadConfig` con los permisos de `seguridad/Pantalla` |
| P-01 Inicio de sesión | `templates/login.html` |
| P-11 Usuarios | `templates/usuarios/` |

`Pantalla` es la única fuente del menú y de los permisos: agregar una pantalla ahí la protege y la muestra a los roles indicados.

## Decisiones por confirmar con el Product Owner

Los documentos del proyecto no definen estos puntos:

- Duración del bloqueo temporal: 15 minutos, contados desde el último intento fallido.
- Política de contraseñas: de 8 a 72 caracteres, con letras y números.
- Página de inicio: Administrador y Ejecutivo llegan a `/tablero`; Consulta, a `/equipos`.
- Rutas web de las pantallas; el diseño solo define las de la API.
- Aviso de sesión 2 minutos antes de expirar, con el botón «Seguir conectado».
- Un administrador no puede desactivarse ni quitarse el rol, y el sistema no puede quedarse sin administradores activos.
- El bloqueo de cuentas vive en memoria: un reinicio del servidor lo levanta.

## Pendiente fuera de este módulo

- API REST con tokens (`/api/v1/auth/login`): el evento PI-01 la exige. Las respuestas JSON 401 y 403 ya están listas para reutilizarse.
- Logotipo oficial: sustituir el texto de `templates/fragmentos/pagina.html` y de `templates/login.html`.
- Bitácora (Sprint 4), catálogo de áreas (P-10) y el resto de las pantallas, que hoy muestran «todavía no está disponible».
- Los documentos de diseño siguen describiendo Laravel; hay que actualizarlos al stack Java.

## Créditos

Las fuentes Atkinson Hyperlegible y Literata se distribuyen con la SIL Open Font License 1.1 (`static/fuentes/OFL-*.txt`).
