-- SS-02 y SS-03: catálogos de tipos de equipo y marcas, y la tabla de equipos (AD 2.2.2).

-- grupo_tecnico no está en el diccionario de datos: indica qué características técnicas
-- se capturan para cada tipo (cómputo, impresión, red, video, energía).
CREATE TABLE tipos_equipo (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(60) NOT NULL,
    vida_util_anios  INT,
    grupo_tecnico    VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tipos_equipo_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipos_equipo_grupo CHECK (grupo_tecnico IN ('COMPUTO', 'IMPRESION', 'RED', 'VIDEO', 'ENERGIA', 'OTRO')),
    CONSTRAINT ck_tipos_equipo_vida CHECK (vida_util_anios IS NULL OR vida_util_anios > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE marcas (
    id     BIGINT      NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_marcas_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- El responsable no se guarda aquí: sale del resguardo activo (3FN, AD 2.2.1).
-- nombre no está en el diccionario de datos: lo pide la Tarea 2 para reconocer el equipo.
CREATE TABLE equipos (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    num_inventario    VARCHAR(30)   NOT NULL,
    num_serie         VARCHAR(60)   NOT NULL,
    nombre            VARCHAR(80)   NOT NULL,
    tipo_equipo_id    BIGINT        NOT NULL,
    marca_id          BIGINT        NOT NULL,
    modelo            VARCHAR(80)   NOT NULL,
    caracteristicas   JSON,
    sistema_operativo VARCHAR(60),
    fecha_adquisicion DATE,
    costo             DECIMAL(12, 2),
    area_id           BIGINT        NOT NULL,
    estado            VARCHAR(20)   NOT NULL,
    qr_uuid           CHAR(36)      NOT NULL,
    observaciones     TEXT,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_equipos_num_inventario UNIQUE (num_inventario),
    CONSTRAINT uk_equipos_num_serie UNIQUE (num_serie),
    CONSTRAINT uk_equipos_qr_uuid UNIQUE (qr_uuid),
    CONSTRAINT fk_equipos_tipo FOREIGN KEY (tipo_equipo_id) REFERENCES tipos_equipo (id),
    CONSTRAINT fk_equipos_marca FOREIGN KEY (marca_id) REFERENCES marcas (id),
    CONSTRAINT fk_equipos_area FOREIGN KEY (area_id) REFERENCES areas (id),
    CONSTRAINT ck_equipos_costo CHECK (costo IS NULL OR costo >= 0),
    CONSTRAINT ck_equipos_estado CHECK (estado IN ('operativo', 'en_reparacion', 'inactivo', 'candidato_baja', 'baja',
                                                   'revision_pendiente'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Catálogos iniciales: el Administrador los podrá ajustar cuando exista la pantalla de catálogos (P-10).
INSERT INTO tipos_equipo (nombre, vida_util_anios, grupo_tecnico) VALUES
    ('Computadora de escritorio', 5, 'COMPUTO'),
    ('Laptop', 5, 'COMPUTO'),
    ('Impresora', 5, 'IMPRESION'),
    ('Multifuncional', 5, 'IMPRESION'),
    ('Switch', 7, 'RED'),
    ('Access point', 5, 'RED'),
    ('Monitor', 6, 'VIDEO'),
    ('Proyector', 5, 'VIDEO'),
    ('No-break', 4, 'ENERGIA');

INSERT INTO marcas (nombre) VALUES
    ('Acer'), ('APC'), ('BenQ'), ('Brother'), ('Cisco'), ('Dell'), ('Epson'), ('HP'), ('Lenovo'), ('TP-Link'), ('Ubiquiti');
