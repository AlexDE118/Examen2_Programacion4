-- =====================================================================
--  BUSCAMINAS — Examen 2 Parcial — Programación 4
--  Script de creación de la base de datos MySQL «minas»
--
--  Uso:
--    mysql -u root -p < minas.sql
--
--  El script es idempotente: puede ejecutarse varias veces.
--  Advertencia: elimina las tablas de la base «minas» si ya existían,
--  reiniciando los datos de prueba.
--
--  Esquema: 2 tablas (usuario, partida) + 1 vista de ranking.
--  Compatible con MySQL 8.0 / MariaDB 10.5 o superior.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. Creación de la base de datos
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `minas`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `minas`;

-- Permite ejecutar el script repetidamente sin errores de clave foránea.
SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS `vw_ranking`;
DROP TABLE IF EXISTS `partida`;
DROP TABLE IF EXISTS `usuario`;

SET FOREIGN_KEY_CHECKS = 1;


-- ---------------------------------------------------------------------
-- 1. Tabla USUARIO
--    Cuentas de acceso de los jugadores. La contraseña se almacena
--    como hash PBKDF2-HMAC-SHA256 en la columna password_hash, nunca en texto plano.
-- ---------------------------------------------------------------------
CREATE TABLE `usuario` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nombre`         VARCHAR(80)     NOT NULL,
  `nombre_usuario` VARCHAR(40)     NOT NULL,
  `email`          VARCHAR(120)    NOT NULL,
  `password_hash`  VARCHAR(100)    NOT NULL,
  `rol`            ENUM('JUGADOR','ADMIN') NOT NULL DEFAULT 'JUGADOR',
  `activo`         BOOLEAN         NOT NULL DEFAULT TRUE,
  `creado_en`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_usuario_email`          (`email`),
  UNIQUE KEY `uq_usuario_nombre_usuario` (`nombre_usuario`),
  CONSTRAINT `chk_usuario_activo` CHECK (`activo` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------
-- 2. Tabla PARTIDA
--    Una fila por partida jugada. El estado completo del tablero se
--    guarda en la columna «tablero» (tipo JSON de MySQL), de modo que
--    no se requiere una tabla adicional por casilla.
--
--    «version» permite el bloqueo optimista (@Version en JPA).
-- ---------------------------------------------------------------------
CREATE TABLE `partida` (
  `id`          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
  `usuario_id`  BIGINT UNSIGNED   NOT NULL,
  `nivel`       VARCHAR(20)       NOT NULL,
  `estado`      ENUM('EN_CURSO','TERMINADA','ABANDONADA') NOT NULL DEFAULT 'EN_CURSO',
  `tiempo_ms`   INT UNSIGNED      NULL,
  `movimientos` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `estrellas`   TINYINT UNSIGNED  NOT NULL DEFAULT 0,
  `tablero`     JSON              NOT NULL,
  `version`     BIGINT UNSIGNED   NOT NULL DEFAULT 0,
  `fecha_inicio` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_fin`   DATETIME          NULL,
  PRIMARY KEY (`id`),
  KEY `idx_partida_usuario` (`usuario_id`),
  KEY `idx_partida_nivel`   (`nivel`, `estado`),
  KEY `idx_partida_fecha`   (`fecha_fin`),
  CONSTRAINT `fk_partida_usuario` FOREIGN KEY (`usuario_id`)
    REFERENCES `usuario` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_partida_nivel`     CHECK (`nivel` IN ('PRINCIPIANTE','INTERMEDIO','EXPERTO')),
  CONSTRAINT `chk_partida_tiempo`   CHECK (`tiempo_ms` IS NULL OR `tiempo_ms` >= 0),
  CONSTRAINT `chk_partida_estrellas` CHECK (`estrellas` BETWEEN 0 AND 3),
  CONSTRAINT `chk_partida_estado`    CHECK (
    (`estado` = 'EN_CURSO'  AND `fecha_fin` IS NULL) OR
    (`estado` <> 'EN_CURSO' AND `fecha_fin` IS NOT NULL)
  )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Un usuario no puede tener dos partidas en curso al mismo tiempo.
-- (MySQL no admite índices únicos parciales; se garantiza en el servicio
--  con una transacción y bloqueo SELECT ... FOR UPDATE sobre «usuario».)


-- ---------------------------------------------------------------------
-- 3. Vista VW_RANKING
--    Consulta analítica del ranking: mejor tiempo (MIN) por usuario y
--    nivel, con el total de partidas terminadas.
--
--    Equivalente en una sola consulta, si el grupo prefiere no usar
--    la vista:
--
--      SELECT u.id, u.nombre_usuario, p.nivel,
--             MIN(p.tiempo_ms) AS mejor_tiempo_ms,
--             COUNT(*)          AS partidas_terminadas
--      FROM partida p
--      JOIN usuario u ON u.id = p.usuario_id
--      WHERE p.estado = 'TERMINADA' AND p.tiempo_ms IS NOT NULL
--      GROUP BY u.id, u.nombre_usuario, p.nivel;
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW `vw_ranking` AS
SELECT
    u.`id`              AS `usuario_id`,
    u.`nombre_usuario`  AS `nombre_usuario`,
    u.`email`           AS `email`,
    p.`nivel`           AS `nivel`,
    MIN(p.`tiempo_ms`)  AS `mejor_tiempo_ms`,
    COUNT(*)            AS `partidas_terminadas`,
    MAX(p.`fecha_fin`)  AS `ultimo_juego`,
    AVG(p.`movimientos`) AS `promedio_movimientos`
FROM `partida` p
JOIN `usuario` u ON u.`id` = p.`usuario_id`
WHERE p.`estado` = 'TERMINADA'
  AND p.`tiempo_ms` IS NOT NULL
GROUP BY u.`id`, u.`nombre_usuario`, u.`email`, p.`nivel`;


-- ---------------------------------------------------------------------
-- 4. Datos iniciales (semilla)
--
--    Nota sobre las contraseñas de prueba: los hashes corresponden a la
--    contraseña «Prueba123$» y usan PBKDF2-HMAC-SHA256 (sal aleatoria de
--    16 bytes y 600 000 iteraciones, valor almacenado en hexadecimal).
--    En la aplicación los grupos deben generar sus propios hashes con el
--    PasswordEncoder de Spring Security; nunca la contraseña en texto plano.
--
--    Los niveles del juego (PRINCIPIANTE 9x9/10, INTERMEDIO 16x16/40,
--    EXPERTO 16x30/99) están definidos como constantes en el backend,
--    por eso no requieren una tabla propia.
-- ---------------------------------------------------------------------
INSERT INTO `usuario` (`nombre`, `nombre_usuario`, `email`, `password_hash`, `rol`) VALUES
  ('Docente Prueba',   'docente',  'docente@minas.local',
   '7a4b794392da7ee15963ef339a2af0332757b9c8a4485767af1579af9c5231f792c87483001d0872b9ee0a18fa722df4', 'ADMIN'),
  ('Jugador de Prueba','jugador1', 'jugador1@minas.local',
   '54c1f0100d3f5c01e877d95270b7b4e8e2a7d46f241fc0410ce80693c8519fc10c4f4ca8f7cf7438256bc54a8e352055', 'JUGADOR');

-- Partida de ejemplo ya terminada, para que el ranking tenga datos
-- desde el primer arranque. El tablero es un documento JSON con la
-- lista de minas reveladas y las casillas descubiertas.
INSERT INTO `partida`
  (`usuario_id`, `nivel`, `estado`, `tiempo_ms`, `movimientos`, `estrellas`, `tablero`, `fecha_inicio`, `fecha_fin`)
SELECT
  u.`id`, 'PRINCIPIANTE', 'TERMINADA', 45000, 38, 3,
  CAST('{"filas":9,"columnas":9,"num_minas":10,"minas":[[0,5],[1,4],[1,7],[2,0],[3,1],[3,6],[5,4],[7,0],[7,6],[8,6]],"reveladas":[[0,0],[0,1],[0,2],[0,3],[0,4],[0,6],[0,7],[0,8],[1,0],[1,1],[1,2],[1,3],[1,5],[1,6],[1,8],[2,1],[2,2],[2,3],[2,4],[2,5],[2,6],[2,7],[2,8],[3,0],[3,2],[3,3],[3,4],[3,5],[3,7],[3,8],[4,0],[4,1],[4,2],[4,3],[4,4],[4,5],[4,6],[4,7],[4,8],[5,0],[5,1],[5,2],[5,3],[5,5],[5,6],[5,7],[5,8],[6,0],[6,1],[6,2],[6,3],[6,4],[6,5],[6,6],[6,7],[6,8],[7,1],[7,2],[7,3],[7,4],[7,5],[7,7],[7,8],[8,0],[8,1],[8,2],[8,3],[8,4],[8,5],[8,7],[8,8]],"banderas":[[0,5],[1,4],[1,7],[2,0],[3,1],[3,6],[5,4],[7,0],[7,6],[8,6]]}' AS JSON),
  TIMESTAMP('2026-11-10 10:00:00'),
  TIMESTAMP('2026-11-10 10:00:45')
FROM `usuario` u
WHERE u.`nombre_usuario` = 'jugador1';

-- =====================================================================
-- FIN DEL SCRIPT
-- =====================================================================
