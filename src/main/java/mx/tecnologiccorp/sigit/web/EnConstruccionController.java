package mx.tecnologiccorp.sigit.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import mx.tecnologiccorp.sigit.seguridad.Pantalla;
import mx.tecnologiccorp.sigit.seguridad.Peticiones;

/** Pantallas de otros incrementos. Sus rutas ya exigen el rol correcto en SeguridadConfig. */
@Controller
public class EnConstruccionController {

	@GetMapping({ "/tablero", "/equipos", "/escaneo", "/resguardos", "/importaciones", "/reportes", "/catalogos",
			"/bitacora" })
	public String mostrar(HttpServletRequest peticion, Model modelo) {
		modelo.addAttribute("titulo", Pantalla.porRuta(Peticiones.ruta(peticion)).orElseThrow().getEtiqueta());
		return "en-construccion";
	}

}
