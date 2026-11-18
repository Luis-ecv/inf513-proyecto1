# 02. Diseño de Base de Datos PostgreSQL

Este documento contiene la especificación completa del modelo relacional físico en PostgreSQL para el servidor `db.tecnoweb.org.bo` (Base de datos: `db_grupo02sa`).

---

## 1. Script DDL Completo (PostgreSQL)

```sql
-- =============================================================================
-- ESQUEMA DE BASE DE DATOS: SEGUIMIENTO AL DISEÑO CURRICULAR
-- GRUPO 02 SA - TECNOWEB
-- =============================================================================

-- Limpieza preventiva en orden inverso a las dependencias
DROP TABLE IF EXISTS evidencia_asignatura CASCADE;
DROP TABLE IF EXISTS evidencia_capitulo CASCADE;
DROP TABLE IF EXISTS documento_evidencia CASCADE;
DROP TABLE IF EXISTS historial_modificacion CASCADE;
DROP TABLE IF EXISTS observacion CASCADE;
DROP TABLE IF EXISTS asignacion_capitulo CASCADE;
DROP TABLE IF EXISTS capitulo CASCADE;
DROP TABLE IF EXISTS estado_capitulo CASCADE;
DROP TABLE IF EXISTS componente CASCADE;
DROP TABLE IF EXISTS prerrequisito CASCADE;
DROP TABLE IF EXISTS asignatura CASCADE;
DROP TABLE IF EXISTS plan_estudio CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;
DROP TABLE IF EXISTS rol CASCADE;

-- -----------------------------------------------------------------------------
-- 1. TABLA: rol (CU1)
-- -----------------------------------------------------------------------------
CREATE TABLE rol (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

-- -----------------------------------------------------------------------------
-- 2. TABLA: usuario (CU1)
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    foto_ruta       VARCHAR(500),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_rol          INT          NOT NULL,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- -----------------------------------------------------------------------------
-- 3. TABLA: plan_estudio (CU2, RF-01)
-- -----------------------------------------------------------------------------
CREATE TABLE plan_estudio (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo              VARCHAR(30)   NOT NULL UNIQUE,
    carrera             VARCHAR(120)  NOT NULL,
    anio_vigencia       INT           NOT NULL CHECK (anio_vigencia >= 1990),
    descripcion         TEXT,
    objetivos           TEXT          NOT NULL,
    perfil_egreso       TEXT          NOT NULL,
    limite_creditos_min INT           NOT NULL DEFAULT 200,
    limite_creditos_max INT           NOT NULL DEFAULT 280,
    estado              VARCHAR(30)   NOT NULL DEFAULT 'En Elaboración'
                        CHECK (estado IN ('En Elaboración', 'Aprobado', 'Histórico', 'Vigente')),
    fecha_creacion      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 4. TABLA: asignatura (CU2, RF-02)
-- -----------------------------------------------------------------------------
CREATE TABLE asignatura (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sigla           VARCHAR(15)  NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    semestre        SMALLINT     NOT NULL CHECK (semestre BETWEEN 1 AND 10),
    horas_teoricas  SMALLINT     NOT NULL DEFAULT 0 CHECK (horas_teoricas >= 0),
    horas_practicas SMALLINT     NOT NULL DEFAULT 0 CHECK (horas_practicas >= 0),
    creditos        SMALLINT     NOT NULL CHECK (creditos > 0),
    area_formativa  VARCHAR(80)  NOT NULL,
    id_plan_estudio INT          NOT NULL,
    CONSTRAINT fk_asignatura_plan FOREIGN KEY (id_plan_estudio)
        REFERENCES plan_estudio (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_asignatura_sigla_plan UNIQUE (sigla, id_plan_estudio)
);

-- -----------------------------------------------------------------------------
-- 5. TABLA: prerrequisito (CU3, RF-03)
-- Grafo de dependencias de materias (DAG)
-- -----------------------------------------------------------------------------
CREATE TABLE prerrequisito (
    id_asignatura          INT NOT NULL,
    id_asignatura_requisito INT NOT NULL,
    PRIMARY KEY (id_asignatura, id_asignatura_requisito),
    CONSTRAINT fk_pre_asig FOREIGN KEY (id_asignatura)
        REFERENCES asignatura (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_pre_requisito FOREIGN KEY (id_asignatura_requisito)
        REFERENCES asignatura (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_no_autorequisito CHECK (id_asignatura <> id_asignatura_requisito)
);

-- -----------------------------------------------------------------------------
-- 6. TABLA: componente (CU4, RF-05)
-- -----------------------------------------------------------------------------
CREATE TABLE componente (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero          SMALLINT     NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     TEXT,
    id_plan_estudio INT          NOT NULL,
    CONSTRAINT fk_componente_plan FOREIGN KEY (id_plan_estudio)
        REFERENCES plan_estudio (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_componente_num_plan UNIQUE (numero, id_plan_estudio)
);

-- -----------------------------------------------------------------------------
-- 7. TABLA: estado_capitulo (CU5, RF-06)
-- Catálogo de la máquina de estados: Borrador, En Revisión, Observado, Aprobado
-- -----------------------------------------------------------------------------
CREATE TABLE estado_capitulo (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

-- -----------------------------------------------------------------------------
-- 8. TABLA: capitulo (CU4, CU5, RF-05, RF-06)
-- -----------------------------------------------------------------------------
CREATE TABLE capitulo (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero          SMALLINT     NOT NULL,
    titulo          VARCHAR(150) NOT NULL,
    contenido       TEXT,
    id_componente   INT          NOT NULL,
    id_estado       INT          NOT NULL DEFAULT 1,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_capitulo_componente FOREIGN KEY (id_componente)
        REFERENCES componente (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_capitulo_estado FOREIGN KEY (id_estado)
        REFERENCES estado_capitulo (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_capitulo_num_componente UNIQUE (numero, id_componente)
);

-- -----------------------------------------------------------------------------
-- 9. TABLA: asignacion_capitulo (CU4, RF-05)
-- Responsables de redacción asignados a cada capítulo
-- -----------------------------------------------------------------------------
CREATE TABLE asignacion_capitulo (
    id_capitulo      INT       NOT NULL,
    id_usuario       INT       NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_capitulo, id_usuario),
    CONSTRAINT fk_asig_cap FOREIGN KEY (id_capitulo)
        REFERENCES capitulo (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asig_usu FOREIGN KEY (id_usuario)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 10. TABLA: observacion (CU5, RF-08)
-- -----------------------------------------------------------------------------
CREATE TABLE observacion (
    id                    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comentario            TEXT         NOT NULL,
    fecha_creacion        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respuesta_delegado    TEXT,
    fecha_respuesta       TIMESTAMP,
    resuelta              BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_cierre          TIMESTAMP,
    id_capitulo           INT          NOT NULL,
    id_usuario_observador INT          NOT NULL,
    id_usuario_respuesta  INT,
    CONSTRAINT fk_obs_capitulo FOREIGN KEY (id_capitulo)
        REFERENCES capitulo (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_obs_autoridad FOREIGN KEY (id_usuario_observador)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_obs_delegado FOREIGN KEY (id_usuario_respuesta)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE SET NULL
);

-- -----------------------------------------------------------------------------
-- 11. TABLA: historial_modificacion (CU5, RF-10)
-- Auditoría inmutable de transiciones de estado
-- -----------------------------------------------------------------------------
CREATE TABLE historial_modificacion (
    id                    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estado_anterior       VARCHAR(50)  NOT NULL,
    estado_nuevo          VARCHAR(50)  NOT NULL,
    descripcion_accion    VARCHAR(255) NOT NULL,
    justificacion_tecnica TEXT         NOT NULL,
    fecha_hora            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_capitulo           INT          NOT NULL,
    id_usuario            INT          NOT NULL,
    CONSTRAINT fk_hist_capitulo FOREIGN KEY (id_capitulo)
        REFERENCES capitulo (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_hist_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- -----------------------------------------------------------------------------
-- 12. TABLA: documento_evidencia (CU6, RF-11, RF-12)
-- Repositorio de evidencias con jerarquía normativa autorreferencial
-- -----------------------------------------------------------------------------
CREATE TABLE documento_evidencia (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_norma        VARCHAR(80)  NOT NULL UNIQUE,
    titulo              VARCHAR(200) NOT NULL,
    tipo_documento      VARCHAR(60)  NOT NULL, -- 'Resolución ICU', 'Dictamen', 'Acta', etc.
    organo_emisor       VARCHAR(120) NOT NULL, -- 'ICU', 'Decanato', 'Consejo Facultativo'
    fecha_emision       DATE         NOT NULL,
    archivo_ruta        VARCHAR(500) NOT NULL,
    vigente             BOOLEAN      NOT NULL DEFAULT TRUE,
    id_documento_padre  INT,
    fecha_registro      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_padre FOREIGN KEY (id_documento_padre)
        REFERENCES documento_evidencia (id) ON UPDATE CASCADE ON DELETE SET NULL
);

-- -----------------------------------------------------------------------------
-- 13. TABLA: evidencia_capitulo (CU6, RF-07, RF-13)
-- Vínculo obligatorio de respaldos para habilitar la aprobación del capítulo
-- -----------------------------------------------------------------------------
CREATE TABLE evidencia_capitulo (
    id_capitulo            INT       NOT NULL,
    id_documento_evidencia INT       NOT NULL,
    fecha_vinculacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_capitulo, id_documento_evidencia),
    CONSTRAINT fk_evi_cap FOREIGN KEY (id_capitulo)
        REFERENCES capitulo (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_evi_doc FOREIGN KEY (id_documento_evidencia)
        REFERENCES documento_evidencia (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- -----------------------------------------------------------------------------
-- 14. TABLA: evidencia_asignatura (CU6)
-- -----------------------------------------------------------------------------
CREATE TABLE evidencia_asignatura (
    id_asignatura          INT       NOT NULL,
    id_documento_evidencia INT       NOT NULL,
    fecha_vinculacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_asignatura, id_documento_evidencia),
    CONSTRAINT fk_evi_asig FOREIGN KEY (id_asignatura)
        REFERENCES asignatura (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_evi_asig_doc FOREIGN KEY (id_documento_evidencia)
        REFERENCES documento_evidencia (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- =============================================================================
-- ÍNDICES ESTRATÉGICOS (Búsqueda Facetada RF-14 y Consultas de Alto Tráfico)
-- =============================================================================
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_asignatura_semestre ON asignatura(id_plan_estudio, semestre);
CREATE INDEX idx_capitulo_estado ON capitulo(id_estado);
CREATE INDEX idx_observacion_capitulo ON observacion(id_capitulo, resuelta);
CREATE INDEX idx_historial_capitulo ON historial_modificacion(id_capitulo);
CREATE INDEX idx_doc_facetada ON documento_evidencia(tipo_documento, organo_emisor, fecha_emision);

-- =============================================================================
-- DATOS SEMILLA (SEEDS)
-- =============================================================================

-- Roles del Sistema (CU1)
INSERT INTO rol (nombre, descripcion) VALUES
('Director', 'Director de Carrera y autoridades académicas con facultades de aprobación'),
('Delegado', 'Miembros de la comisión de rediseño curricular y redactores'),
('Invitado', 'Docentes y observadores externos con permisos de solo lectura');

-- Estados de la Máquina de Estados (CU5, RF-06)
INSERT INTO estado_capitulo (nombre, descripcion) VALUES
('Borrador', 'En etapa inicial de redacción por parte de los delegados asignados'),
('En Revisión', 'Sometido a evaluación formal por la comisión o autoridades'),
('Observado', 'Presenta observaciones técnicas pendientes de subsanación'),
('Aprobado', 'Validado formalmente con observaciones subsanadas y evidencias adjuntas');

-- Usuario Administrador / Director Inicial
INSERT INTO usuario (nombre_completo, email, username, password, foto_ruta, id_rol) VALUES
('Ing. Evans Balcazar', 'grupo02sa@tecnoweb.org.bo', 'director', 'admin123', 'fotos/director.png', 1);
```
