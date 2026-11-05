import Modal from 'bootstrap/js/dist/modal';

const CLAVE = 'sigit:ultima-actividad';
const MARGEN_MS = 5000;

// La última actividad se comparte entre pestañas. Sin localStorage, cuenta solo esta pestaña.
let ultimaActividadLocal = Date.now();

function leerUltimaActividad() {
    try {
        return Math.max(Number(localStorage.getItem(CLAVE)) || 0, ultimaActividadLocal);
    } catch {
        return ultimaActividadLocal;
    }
}

function registrarActividad() {
    ultimaActividadLocal = Date.now();

    try {
        localStorage.setItem(CLAVE, String(ultimaActividadLocal));
    } catch {
        // Modo privado o almacenamiento bloqueado.
    }
}

export function iniciarAvisoDeSesion() {
    const datos = document.body.dataset;
    const elemento = document.getElementById('modal-sesion');

    if (!datos.sesionMinutos || !elemento) {
        return;
    }

    const duracion = Number(datos.sesionMinutos) * 60000;
    const aviso = duracion - Number(datos.sesionAvisoMinutos) * 60000;
    const modal = new Modal(elemento);
    let visible = false;

    const expirar = () => {
        window.location.href = datos.sesionUrlExpirada;
    };

    const revisar = () => {
        const inactividad = Date.now() - leerUltimaActividad();

        if (inactividad >= duracion + MARGEN_MS) {
            expirar();
        } else if (inactividad >= aviso && !visible) {
            visible = true;
            modal.show();
        } else if (inactividad < aviso && visible) {
            visible = false;
            modal.hide();
        }
    };

    document.getElementById('sesion-seguir').addEventListener('click', async () => {
        try {
            const respuesta = await fetch(datos.sesionUrlMantener, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]').content,
                },
            });

            if (!respuesta.ok) {
                expirar();
                return;
            }

            registrarActividad();
            revisar();
        } catch {
            // Sin red: el aviso sigue visible para que la persona pueda reintentar.
        }
    });

    registrarActividad();
    setInterval(revisar, 10000);
    // Los temporizadores se detienen con el celular bloqueado: se revisa al volver.
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            revisar();
        }
    });
    window.addEventListener('storage', revisar);
}
