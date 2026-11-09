-- Tarea 3: edición de equipos. version no está en el diccionario de datos: evita que dos personas
-- que editan el mismo equipo a la vez se sobrescriban sin darse cuenta (bloqueo optimista).
ALTER TABLE equipos ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Orden del listado (P-03) por nombre y por fecha de registro.
CREATE INDEX ix_equipos_nombre ON equipos (nombre);
CREATE INDEX ix_equipos_created_at ON equipos (created_at);
