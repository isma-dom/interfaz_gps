-- SS-01 Seguridad: roles, áreas y usuarios (SIGIT-DMS-A3.2-AD, 2.2.2).

CREATE TABLE roles (
    id     BIGINT      NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Solo la estructura: el catálogo de áreas (P-10) es de otra historia.
CREATE TABLE areas (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    nombre   VARCHAR(100) NOT NULL,
    edificio VARCHAR(60),
    piso     VARCHAR(20),
    PRIMARY KEY (id),
    CONSTRAINT uk_areas_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    rol_id     BIGINT       NOT NULL,
    area_id    BIGINT,      -- solo el rol Consulta tiene área
    nombre     VARCHAR(120) NOT NULL,
    correo     VARCHAR(150) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_usuarios_area FOREIGN KEY (area_id) REFERENCES areas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO roles (nombre) VALUES ('Administrador'), ('Ejecutivo'), ('Consulta');
