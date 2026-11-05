import '@fontsource/atkinson-hyperlegible/latin-400.css';
import '@fontsource/atkinson-hyperlegible/latin-700.css';
import '@fontsource-variable/literata/index.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import 'bootstrap/js/dist/offcanvas';

import { iniciarAvisoDeSesion } from './sesion';
import { iniciarFormularios } from './formularios';
import { iniciarUsuarios } from './usuarios';

iniciarFormularios();
iniciarAvisoDeSesion();
iniciarUsuarios();
