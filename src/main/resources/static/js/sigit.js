/*
 * Comportamiento de la interfaz de SIGIT. Usa el Bootstrap que carga cabeza.html (window.bootstrap).
 */
(function () {
    'use strict';

    /* ---------- Formularios: doble envío y mostrar contraseña ---------- */

    document.addEventListener('submit', function (evento) {
        var formulario = evento.target;
        if (formulario.method !== 'post' || evento.defaultPrevented) {
            return;
        }
        // Se desactivan después de que el navegador arma el envío: un botón desactivado durante
        // el evento no envía su name (por ejemplo, «Registrar y agregar otro»).
        setTimeout(function () {
            formulario.querySelectorAll('[type="submit"]').forEach(function (boton) {
                boton.disabled = true;
            });
        }, 0);
    });

    // Al volver con el botón Atrás, el navegador puede restaurar los botones desactivados.
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form [type="submit"]:disabled').forEach(function (boton) {
            boton.disabled = false;
        });
    });

    document.querySelectorAll('[data-ver-contrasena]').forEach(function (boton) {
        var campo = document.getElementById(boton.dataset.verContrasena);
        var icono = boton.querySelector('.bi');
        boton.addEventListener('click', function () {
            var mostrar = campo.type === 'password';
            campo.type = mostrar ? 'text' : 'password';
            boton.setAttribute('aria-pressed', String(mostrar));
            boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
            icono.classList.toggle('bi-eye', !mostrar);
            icono.classList.toggle('bi-eye-slash', mostrar);
        });
    });

    /* ---------- Aviso de sesión por expirar ---------- */

    var CLAVE = 'sigit:ultima-actividad';
    var MARGEN_MS = 5000;
    // La última actividad se comparte entre pestañas. Sin localStorage, cuenta solo esta pestaña.
    var ultimaActividadLocal = Date.now();

    function leerUltimaActividad() {
        try {
            return Math.max(Number(localStorage.getItem(CLAVE)) || 0, ultimaActividadLocal);
        } catch (e) {
            return ultimaActividadLocal;
        }
    }

    function registrarActividad() {
        ultimaActividadLocal = Date.now();
        try {
            localStorage.setItem(CLAVE, String(ultimaActividadLocal));
        } catch (e) {
            // Modo privado o almacenamiento bloqueado.
        }
    }

    function iniciarAvisoDeSesion() {
        var datos = document.body.dataset;
        var elemento = document.getElementById('modal-sesion');
        if (!datos.sesionMinutos || !elemento) {
            return;
        }

        var duracion = Number(datos.sesionMinutos) * 60000;
        var aviso = duracion - Number(datos.sesionAvisoMinutos) * 60000;
        var modal = new bootstrap.Modal(elemento);
        var visible = false;

        function expirar() {
            window.location.href = datos.sesionUrlExpirada;
        }

        function revisar() {
            var inactividad = Date.now() - leerUltimaActividad();
            if (inactividad >= duracion + MARGEN_MS) {
                expirar();
            } else if (inactividad >= aviso && !visible) {
                visible = true;
                modal.show();
            } else if (inactividad < aviso && visible) {
                visible = false;
                modal.hide();
            }
        }

        document.getElementById('sesion-seguir').addEventListener('click', function () {
            var token = document.querySelector('meta[name="csrf-token"]');
            fetch(datos.sesionUrlMantener, {
                method: 'POST',
                headers: { 'Accept': 'application/json', 'X-CSRF-TOKEN': token ? token.content : '' },
                credentials: 'same-origin'
            }).then(function (respuesta) {
                if (!respuesta.ok) {
                    expirar();
                    return;
                }
                registrarActividad();
                revisar();
            }).catch(function () {
                // Sin red: el aviso sigue visible para que la persona pueda reintentar.
            });
        });

        registrarActividad();
        setInterval(revisar, 10000);
        // Los temporizadores se detienen con el celular bloqueado: se revisa al volver.
        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) {
                revisar();
            }
        });
        window.addEventListener('storage', revisar);
    }

    /* ---------- P-11: usuarios ---------- */

    // El área solo aplica al rol Consulta.
    function iniciarCampoArea() {
        var rol = document.getElementById('rolId');
        var contenedor = document.getElementById('campo-area');
        if (!rol || !contenedor) {
            return;
        }
        var area = document.getElementById('areaId');
        var rolConsulta = contenedor.dataset.rolConsulta;

        function actualizar() {
            var esConsulta = rol.value === rolConsulta;
            contenedor.hidden = !esConsulta;
            area.required = esConsulta;
        }

        if (rol.tagName === 'SELECT') {
            rol.addEventListener('change', actualizar);
        }
        actualizar();
    }

    // Un solo modal de confirmación para todas las filas.
    function iniciarConfirmacion() {
        var elemento = document.getElementById('modal-desactivar');
        if (!elemento) {
            return;
        }
        elemento.addEventListener('show.bs.modal', function (evento) {
            var boton = evento.relatedTarget;
            elemento.querySelector('form').action = boton.dataset.url;
            elemento.querySelector('[data-nombre]').textContent = boton.dataset.nombre;
        });
    }

    /* ---------- P-05: formulario de equipo ---------- */

    // Muestra solo las características técnicas del tipo de equipo elegido.
    function iniciarCaracteristicasPorTipo() {
        var tipo = document.querySelector('[data-tipo-equipo]');
        if (!tipo) {
            return;
        }
        var grupos = document.querySelectorAll('fieldset [data-grupo]');
        var indicacion = document.querySelector('[data-sin-tipo]');

        tipo.addEventListener('change', function () {
            var opcion = tipo.selectedOptions[0];
            var grupo = opcion && opcion.dataset.grupoTecnico;
            grupos.forEach(function (bloque) {
                bloque.hidden = bloque.dataset.grupo !== grupo;
            });
            indicacion.hidden = Boolean(grupo);
        });
    }

    // Debajo del área se ve el edificio y el piso, para confirmar la ubicación.
    function iniciarDetalleDeArea() {
        var area = document.getElementById('areaId');
        var detalle = document.querySelector('[data-detalle-area]');
        if (!area || !detalle) {
            return;
        }
        var original = detalle.textContent;
        function actualizar() {
            var opcion = area.selectedOptions[0];
            detalle.textContent = opcion && opcion.dataset.detalle ? opcion.dataset.detalle : original;
        }
        area.addEventListener('change', actualizar);
        actualizar();
    }

    // Con errores, el foco va al resumen para que se lea primero.
    function enfocarResumenDeErrores() {
        var resumen = document.getElementById('resumen-errores');
        if (resumen) {
            resumen.focus();
        }
    }

    /* ---------- P-03: selección para imprimir etiquetas ---------- */

    function iniciarSeleccionDeEquipos() {
        var barra = document.getElementById('barra-seleccion');
        if (!barra) {
            return;
        }
        var casillas = Array.prototype.slice.call(document.querySelectorAll('[data-seleccion-equipo]'));
        var todos = document.getElementById('seleccionar-todos');
        var conteo = document.getElementById('conteo-seleccion');

        function seleccionados() {
            return casillas.filter(function (c) { return c.checked; });
        }

        function texto(n) {
            return n === 1 ? '1 equipo seleccionado' : n + ' equipos seleccionados';
        }

        function actualizar() {
            var n = seleccionados().length;
            barra.hidden = n === 0;
            conteo.textContent = texto(n);
            if (todos) {
                todos.checked = n > 0 && n === casillas.length;
                todos.indeterminate = n > 0 && n < casillas.length;
            }
        }

        casillas.forEach(function (c) { c.addEventListener('change', actualizar); });
        if (todos) {
            todos.addEventListener('change', function () {
                casillas.forEach(function (c) { c.checked = todos.checked; });
                actualizar();
            });
        }
        document.getElementById('quitar-seleccion').addEventListener('click', function () {
            casillas.forEach(function (c) { c.checked = false; });
            actualizar();
        });

        var modal = document.getElementById('modal-etiquetas');
        modal.addEventListener('show.bs.modal', function () {
            var numeros = seleccionados().map(function (c) { return c.value; });
            modal.querySelector('[data-conteo-etiquetas]').textContent =
                numeros.length === 1 ? '1 etiqueta' : numeros.length + ' etiquetas';
            modal.querySelector('[data-lista-etiquetas]').textContent = numeros.join(', ');
        });
    }

    function iniciarAvisoPendiente() {
        var modal = document.getElementById('modal-pendiente');
        if (!modal) {
            return;
        }
        modal.addEventListener('show.bs.modal', function (evento) {
            modal.querySelector('[data-texto-pendiente]').textContent = evento.relatedTarget.dataset.accion;
        });
    }

    iniciarAvisoDeSesion();
    iniciarCampoArea();
    iniciarConfirmacion();
    iniciarCaracteristicasPorTipo();
    iniciarDetalleDeArea();
    enfocarResumenDeErrores();
    iniciarSeleccionDeEquipos();
    iniciarAvisoPendiente();
})();
