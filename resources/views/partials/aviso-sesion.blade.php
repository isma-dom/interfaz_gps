<div class="modal fade" id="modal-sesion" tabindex="-1" data-bs-backdrop="static" data-bs-keyboard="false" aria-labelledby="modal-sesion-titulo" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title fs-5" id="modal-sesion-titulo">Tu sesión está por terminar</h2>
            </div>
            <div class="modal-body">
                Se cerrará en {{ config('sigit.aviso_sesion_minutos') }} minutos por inactividad. Lo que no hayas guardado se perderá.
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" id="sesion-seguir">Seguir conectado</button>
            </div>
        </div>
    </div>
</div>
