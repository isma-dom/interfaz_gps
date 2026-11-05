import Modal from 'bootstrap/js/dist/modal';

// El área solo aplica al rol Consulta.
function iniciarCampoArea() {
    const rol = document.getElementById('rol_id');
    const contenedor = document.getElementById('campo-area');

    if (!rol || !contenedor) {
        return;
    }

    const area = document.getElementById('area_id');
    const rolConsulta = contenedor.dataset.rolConsulta;

    const actualizar = () => {
        const esConsulta = rol.value === rolConsulta;

        contenedor.hidden = !esConsulta;
        area.required = esConsulta;
    };

    if (rol.tagName === 'SELECT') {
        rol.addEventListener('change', actualizar);
    }

    actualizar();
}

// Un solo modal de confirmación para todas las filas.
function iniciarConfirmacion() {
    const elemento = document.getElementById('modal-desactivar');

    if (!elemento) {
        return;
    }

    elemento.addEventListener('show.bs.modal', (evento) => {
        const { url, nombre } = evento.relatedTarget.dataset;

        elemento.querySelector('form').action = url;
        elemento.querySelector('[data-nombre]').textContent = nombre;
    });

    new Modal(elemento);
}

export function iniciarUsuarios() {
    iniciarCampoArea();
    iniciarConfirmacion();
}
