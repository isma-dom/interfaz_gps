package mx.tecnologiccorp.sigit.inventario;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/**
 * Qué equipos puede ver cada usuario (C-04): todos los roles consultan, pero Consulta solo los de su área.
 * Para Consulta, un equipo de otra área «no existe» (404), llegue por la ficha o por el código QR.
 */
@Component
public class AccesoAEquipos {

	private final EquipoRepository equipos;

	private final UsuarioRepository usuarios;

	public AccesoAEquipos(EquipoRepository equipos, UsuarioRepository usuarios) {
		this.equipos = equipos;
		this.usuarios = usuarios;
	}

	/** El área fija del usuario Consulta; nula para los demás roles o si no tiene área. */
	@Transactional(readOnly = true)
	public Area areaDe(UsuarioSesion usuario) {
		return usuarios.findById(usuario.id()).map(u -> u.getArea()).orElse(null);
	}

	@Transactional(readOnly = true)
	public Equipo visible(Long id, UsuarioSesion usuario) {
		return comprobar(equipos.findById(id).orElseThrow(() -> new EquipoNoEncontrado(id)), usuario);
	}

	/** El equipo de un código QR escaneado (C-14). */
	@Transactional(readOnly = true)
	public Equipo visiblePorQr(String qrUuid, UsuarioSesion usuario) {
		Equipo equipo = equipos.findByQrUuid(qrUuid == null ? "" : qrUuid.trim().toLowerCase())
			.orElseThrow(() -> new EquipoNoEncontrado(null));
		return comprobar(equipo, usuario);
	}

	private Equipo comprobar(Equipo equipo, UsuarioSesion usuario) {
		if (usuario.tieneRol(Rol.CONSULTA)) {
			Area area = areaDe(usuario);
			if (area == null || !area.getId().equals(equipo.getArea().getId())) {
				throw new EquipoNoEncontrado(equipo.getId());
			}
		}
		return equipo;
	}

}
