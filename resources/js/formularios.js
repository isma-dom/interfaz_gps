// Comportamientos comunes de formularios: doble envío y mostrar contraseña.
export function iniciarFormularios() {
    document.addEventListener('submit', (evento) => {
        const formulario = evento.target;

        if (formulario.method !== 'post' || evento.defaultPrevented) {
            return;
        }

        formulario.querySelectorAll('[type="submit"]').forEach((boton) => {
            boton.disabled = true;
        });
    });

    // Al volver con el botón Atrás, el navegador puede restaurar los botones desactivados.
    window.addEventListener('pageshow', () => {
        document.querySelectorAll('form [type="submit"]:disabled').forEach((boton) => {
            boton.disabled = false;
        });
    });

    document.querySelectorAll('[data-ver-contrasena]').forEach((boton) => {
        const campo = document.getElementById(boton.dataset.verContrasena);
        const icono = boton.querySelector('.bi');

        boton.addEventListener('click', () => {
            const mostrar = campo.type === 'password';

            campo.type = mostrar ? 'text' : 'password';
            boton.setAttribute('aria-pressed', String(mostrar));
            boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
            icono.classList.toggle('bi-eye', !mostrar);
            icono.classList.toggle('bi-eye-slash', mostrar);
        });
    });
}
