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

El Administrador registra equipos en Equipos → Nuevo equipo (http://localhost:8080/equipos/nuevo). Se guardan en la tabla `equipos` de MySQL, creada por la migración `V2__inventario.sql` con los catálogos iniciales de tipos y marcas. Antes del primer alta tiene que existir al menos un área.

## Consulta y actualización de equipos (Tarea 3)

- **Listado** (http://localhost:8080/equipos): todos los roles consultan los equipos guardados en MySQL, de 15 en 15. El buscador localiza un equipo por número de inventario, número de serie, nombre o modelo (sin importar mayúsculas ni acentos) o por el código QR completo; hay filtros por área, tipo y estado, y orden por inventario, nombre o fecha de registro. Consulta solo ve su área.
- **Ficha** (`/equipos/{id}`): toda la información del equipo, con sus características técnicas.
- **Edición** (`/equipos/{id}/editar`, solo Administrador): las mismas reglas que el alta. El estado y el código QR no se editan ahí, y un equipo dado de baja ya no admite cambios. Al guardar, la ficha confirma qué datos cambiaron y los marca.
- **Edición simultánea**: la columna `equipos.version` (migración `V3__edicion_equipos.sql`) evita que alguien sobrescriba sin darse cuenta los cambios que otra persona guardó mientras editaba.

## Baja de equipos (Tarea 4)

- **Quién y cuándo**: solo el Administrador, y solo para equipos Candidato a baja o en Revisión pendiente (máquina de estados de AD 2.2.3). Desde la ficha, «Dar de baja» abre una página de confirmación (`/equipos/{id}/baja`) que pide el motivo por escrito y marcar que la baja es definitiva (RN-04).
- **Qué guarda**: el equipo pasa a estado `baja` (no se borra) y la tabla `movimientos` (migración `V4__movimientos.sql`) registra quién, cuándo, el estado anterior y el motivo. El alta también deja su movimiento.
- **Después**: el equipo deja de aparecer en el listado de activos; se consulta con el filtro «Dados de baja», su ficha conserva todos los datos con su historial y ya no admite cambios.
- **Candidatos a baja (RN-06)**: `ClasificadorDeObsolescencia` corre al arrancar y cada día a las 2:00. Pasa a Candidato a baja los equipos Operativos o Inactivos con 5 años o más de antigüedad o con un sistema operativo sin soporte (`sigit.obsolescencia.*` en `application.properties`).

## Códigos QR (Tarea 5)

- **Qué lleva**: solo la URL `<servidor>/e/<qr_uuid>` (RN-05, C-26). El `qr_uuid` se asigna al registrar el equipo, es único en la base de datos y no cambia al editarlo. Define `SIGIT_URL_PUBLICA` (por ejemplo `https://sigit.bbj.gob.mx`) para que los códigos lleven la dirección pública; sin ella se usa la dirección con la que se pidió la página.
- **Escaneo**: la cámara del celular abre `/e/<qr_uuid>`; sin sesión, SIGIT pide iniciarla y luego abre la ficha del equipo. También responde `/equipos/qr/<qr_uuid>`. Consulta solo abre los de su área.
- **Ver y descargar**: la ficha muestra el código y lo descarga en PNG o SVG (`/equipos/{id}/qr.png`, `/equipos/{id}/qr.svg`).
- **Imprimir** (solo Administrador): etiquetas de 5 × 3 cm en PDF tamaño carta, 24 por hoja, con el código, el número de inventario y el área. Una desde la ficha (`/equipos/{id}/etiqueta.pdf`) o varias seleccionándolas en el listado (`/equipos/etiquetas?id=…`). Los equipos dados de baja ya no llevan etiqueta.
- Librerías: ZXing 3.5.4 (genera el código) y OpenPDF 3.0.5 (el PDF).

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
- Columnas que no están en el diccionario de datos: `equipos.nombre` (Tarea 2), `tipos_equipo.grupo_tecnico` (Tarea 2) y `equipos.version` (Tarea 3). En `movimientos`, `usuario_id` admite nulo para la clasificación automática y para las altas anteriores a la tabla (Tarea 4).
- Los parámetros de obsolescencia viven en `application.properties` hasta que exista la tabla `parametros` y su pantalla.
- La edición de equipos todavía no deja rastro en la bitácora: se agrega con la Bitácora (Sprint 4).
- El bloqueo de cuentas vive en memoria: un reinicio del servidor lo levanta.

## Pendiente fuera de este módulo

- API REST con tokens (`/api/v1/auth/login`): el evento PI-01 la exige. Las respuestas JSON 401 y 403 ya están listas para reutilizarse.
- Logotipo oficial: sustituir el texto de `templates/fragmentos/pagina.html` y de `templates/login.html`.
- Bitácora (Sprint 4), catálogo de áreas (P-10) y el resto de las pantallas, que hoy muestran «todavía no está disponible».
- Los documentos de diseño siguen describiendo Laravel; hay que actualizarlos al stack Java.

## Créditos

Las fuentes Atkinson Hyperlegible y Literata se distribuyen con la SIL Open Font License 1.1 (`static/fuentes/OFL-*.txt`).
