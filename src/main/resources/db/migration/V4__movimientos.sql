-- Tarea 4: historial de cada equipo (tabla movimientos, AD 2.2.2). La baja es un estado y queda
-- registrada aquí: ningún equipo se borra.
-- usuario_id admite nulo: lo deja vacío la clasificación automática de obsolescencia (RN-06) y las
-- altas anteriores a esta tabla, de las que no se sabe quién las registró.
CREATE TABLE movimientos (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    equipo_id       BIGINT      NOT NULL,
    usuario_id      BIGINT,
    tipo            VARCHAR(20) NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20),
    area_origen_id  BIGINT,
    area_destino_id BIGINT,
    motivo          TEXT,
    fecha           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_movimientos_equipo FOREIGN KEY (equipo_id) REFERENCES equipos (id),
    CONSTRAINT fk_movimientos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_movimientos_area_origen FOREIGN KEY (area_origen_id) REFERENCES areas (id),
    CONSTRAINT fk_movimientos_area_destino FOREIGN KEY (area_destino_id) REFERENCES areas (id),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo IN ('alta', 'reasignacion', 'envio_reparacion', 'retorno', 'cambio_estado',
                                                   'baja')),
    CONSTRAINT ck_movimientos_estado_anterior CHECK (estado_anterior IS NULL OR estado_anterior IN ('operativo',
        'en_reparacion', 'inactivo', 'candidato_baja', 'baja', 'revision_pendiente')),
    CONSTRAINT ck_movimientos_estado_nuevo CHECK (estado_nuevo IS NULL OR estado_nuevo IN ('operativo', 'en_reparacion',
        'inactivo', 'candidato_baja', 'baja', 'revision_pendiente')),
    -- RN-04: toda baja lleva su motivo por escrito. El IS NOT NULL es necesario: con motivo nulo, la
    -- longitud da NULL y MySQL acepta un CHECK que no da falso.
    CONSTRAINT ck_movimientos_motivo_baja CHECK (tipo <> 'baja' OR (motivo IS NOT NULL AND CHAR_LENGTH(TRIM(motivo)) >= 10))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_movimientos_equipo_fecha ON movimientos (equipo_id, fecha);

-- Los equipos que ya existían reciben su movimiento de alta, para que su historial empiece completo.
INSERT INTO movimientos (equipo_id, tipo, estado_nuevo, area_destino_id, fecha)
SELECT id, 'alta', 'operativo', area_id, created_at FROM equipos;
