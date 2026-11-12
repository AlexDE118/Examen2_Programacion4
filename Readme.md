# Minas — Buscaminas web (Examen 2 Parcial, Programación 4)

Aplicación full-stack de Buscaminas: **Spring Boot 3.5 (REST + JPA + Security)**, **MySQL 8** y **React 18 por CDN** servido como estático por el propio backend. Todo el juego se resuelve en el servidor: el navegador nunca recibe la posición de las minas mientras la partida está en curso.

- Proyecto Maven: `com.bachillerato:buscaminas` (jar ejecutable)
- Una sola URL: <http://localhost:8080>

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 o superior |
| Maven | 3.9+ (o el que trae el IDE) |
| MySQL | 8.0 |

No se necesita Node.js, npm ni ningún empaquetador. El navegador descarga React 18.3.1 y Babel desde unpkg, así que requiere conexión a internet.

## 1. Base de datos

Desde la raíz del proyecto:

```bash
mysql -u root -p < minas.sql
```

El script es idempotente: crea la base `minas` (utf8mb4), las tablas `usuario` y `partida`, la vista `vw_ranking` y los datos de prueba. Si se vuelve a ejecutar, la base queda reiniciada.

Comprobación rápida:

```sql
USE minas;
SHOW CREATE TABLE partida;
SELECT * FROM vw_ranking;
```

## 2. Configuración de la conexión

`src/main/resources/application.properties` lee las credenciales desde variables de entorno:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | servidor MySQL |
| `DB_PORT` | `3306` | puerto |
| `DB_USER` | `root` | usuario MySQL |
| `DB_PASSWORD` | `root` | contraseña MySQL |

URL JDBC resultante: `jdbc:mysql://localhost:3306/minas?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=America/Costa_Rica`

Credenciales de MySQL usadas en desarrollo: **usuario `root`, contraseña `root`**. Si su MySQL usa otra contraseña, defina la variable antes de arrancar:

```bash
# Linux / macOS
export DB_PASSWORD=su_clave

# Windows PowerShell
$env:DB_PASSWORD="su_clave"
```

## 3. Ejecutar backend y frontend (una sola aplicación, puerto 8080)

```bash
# en la carpeta examen2-respuesta/ (donde está pom.xml)
mvn spring-boot:run
```

Abrir en el navegador: <http://localhost:8080>

Alternativa con jar:

```bash
mvn clean package
java -jar target/buscaminas-1.0.0.jar
```

## 4. Credenciales de prueba

| Usuario | Email | Contraseña | Rol |
|---|---|---|---|
| `docente` | docente@minas.local | `Prueba123$` | ADMIN |
| `jugador1` | jugador1@minas.local | `Prueba123$` | JUGADOR |

Se puede iniciar sesión con el nombre de usuario o con el email. Para demostrar el registro real, cree una cuenta nueva desde la pantalla «Regístrese».

## 5. Pruebas automatizadas

```bash
mvn test
```

- `MotorBuscaminasTest`: colocación de minas, conteo de adyacentes, **apertura en cadena**, banderas que frenan la cadena, victoria, derrota y acciones inválidas.
- `PartidaServiceImplTest`: prueba de la capa de servicio con repositorios simulados (Mockito). Verifica que revelar una casilla vacía abre en cadena, que el JSON queda actualizado, que la respuesta no contiene minas y que se aplican 403, 404 y 400.
- `BuscaminasApplicationTests`: levanta el contexto completo contra MySQL. Necesita la base creada, así que solo corre con `MINAS_IT=true mvn test`.

## 6. API REST

Todas las respuestas son JSON. Los errores siguen un formato único:

```json
{ "fecha": "2026-11-10T10:00:00", "status": 400, "error": "Bad Request",
  "mensaje": "Los datos enviados no son válidos", "ruta": "/api/auth/registro",
  "campos": { "password": "La contraseña debe contener al menos una letra y un número" } }
```

| Método | Ruta | Sesión | Respuestas |
|---|---|---|---|
| POST | `/api/auth/registro` | no | 201 usuario creado · 400 validación · 409 email o usuario repetido |
| POST | `/api/auth/login` | no | 200 usuario + cookie de sesión · 401 «Credenciales inválidas» · 429 tras 5 fallos |
| POST | `/api/auth/logout` | sí | 204 · 401 sin sesión |
| GET | `/api/auth/me` | sí | 200 usuario · 401 |
| GET | `/api/niveles` | no | 200 niveles (constantes del backend) |
| POST | `/api/partidas` | sí | 201 tablero inicial filtrado · 401 · 409 ya hay una partida en curso |
| GET | `/api/partidas/actual` | sí | 200 · 404 no hay partida en curso |
| POST | `/api/partidas/{id}/acciones` | sí | 200 estado actualizado · 400 acción inválida o partida inactiva · 403 ajena · 404 |
| POST | `/api/partidas/{id}/abandonar` | sí | 200 · 400 si no está en curso · 403 · 404 (**ruta adicional**) |
| GET | `/api/partidas/{id}` | sí | 200 detalle · 403 ajena · 404 |
| GET | `/api/usuarios/{id}/partidas` | sí | 200 historial · 403 si `{id}` no es el usuario de la sesión |
| GET | `/api/ranking?nivel=` | sí | 200 posiciones por nivel + `miPosicion` (el parámetro `nivel` es opcional) |

Cuerpos de ejemplo:

```json
POST /api/auth/registro
{ "nombre": "Ana Pérez", "nombreUsuario": "ana", "email": "ana@correo.com",
  "password": "Clave1234", "confirmacion": "Clave1234" }

POST /api/auth/login
{ "usuario": "jugador1", "password": "Prueba123$" }

POST /api/partidas
{ "nivel": "PRINCIPIANTE" }            // INTERMEDIO | EXPERTO

POST /api/partidas/5/acciones
{ "tipo": "REVELAR", "fila": 3, "columna": 4 }   // tipo: REVELAR | BANDERA
```

**Celdas filtradas.** `PartidaDTO.celdas` es una matriz `[fila][columna]` de códigos: `O` oculta, `B` bandera, `0`…`8` abierta con su número, y solo cuando la partida terminó: `M` mina, `X` mina pisada, `BI` bandera mal puesta. Mientras la partida está `EN_CURSO` solo pueden aparecer `O`, `B` y números.

Prueba rápida con curl:

```bash
curl -i -c c.txt -H "Content-Type: application/json" \
     -d '{"usuario":"jugador1","password":"Prueba123$"}' http://localhost:8080/api/auth/login
curl -i -b c.txt -H "Content-Type: application/json" -d '{"nivel":"PRINCIPIANTE"}' http://localhost:8080/api/partidas
curl -i -b c.txt -H "Content-Type: application/json" \
     -d '{"tipo":"REVELAR","fila":0,"columna":0}' http://localhost:8080/api/partidas/2/acciones
curl -i http://localhost:8080/api/partidas/actual          # sin cookie -> 401
```

## 7. Arquitectura

```
src/main/java/com/bachillerato/buscaminas/
├── BuscaminasApplication.java
├── config/          SecurityConfig (PBKDF2, sesión, 401/403 JSON), UsuarioAutenticado
├── controller/      AuthController, NivelController, PartidaController, UsuarioController, RankingController
├── service/         interfaces AuthService, PartidaService, RankingService
│   └── impl/        implementaciones + MotorBuscaminas (reglas), TableroMapper, PartidaMapper,
│                    UsuarioDetailsService, IntentosLoginService
├── repository/      UsuarioRepository, PartidaRepository (JPQL + SQL nativo sobre vw_ranking)
├── model/entity/    Usuario, Partida (@ManyToOne), Rol, EstadoPartida
├── model/dto/       DTO de entrada (Bean Validation) y de salida
├── model/juego/     Nivel (constantes), Tablero, TableroJson, TipoAccion, ResultadoPartida
└── exception/       ApiException + GlobalExceptionHandler
src/main/resources/static/
├── index.html       carga React 18 + Babel desde CDN
├── css/styles.css
└── js/api.js, js/app.jsx, js/componentes/{FormularioLogin, FormularioRegistro, Tablero, PanelHistorial, TablaRanking}.jsx
```

Flujo de una jugada: `Tablero.jsx` → `fetch POST /api/partidas/{id}/acciones` → `PartidaController` → `PartidaServiceImpl` (`@Transactional`: carga la partida, verifica el dueño, lee el JSON, aplica `MotorBuscaminas`, guarda) → `PartidaMapper` filtra las celdas → JSON al navegador.

### Seguridad

- Contraseñas: `Pbkdf2PasswordEncoder("", 16, 600_000, PBKDF2WithHmacSHA256)`. Se guardan 96 caracteres hex (sal + hash).
- Sesión HTTP en la cookie `MINASSESSION` (HttpOnly, SameSite=Strict, 30 min). Al iniciar sesión se crea una sesión nueva, para evitar la fijación de sesión. El logout la invalida en el servidor.
- Login con mensaje genérico «Credenciales inválidas» tanto si el usuario no existe como si la contraseña es incorrecta o la cuenta está inactiva. Tras 5 intentos fallidos por usuario e IP se bloquea 15 minutos (429).
- Control por propietario: cada partida e historial se compara con el id de la sesión y responde 403 si es de otro usuario.
- Los errores inesperados devuelven 500 con un mensaje genérico, sin trazas.

### Persistencia

- Con `ddl-auto=none`, Hibernate no modifica el esquema: lo define `minas.sql`.
- `partida.tablero` se mapea como `String` con `@Column(columnDefinition = "json")`. El servicio lo convierte con Jackson (`TableroMapper`) y conserva el formato de la partida semilla (`filas`, `columnas`, `num_minas`, `minas`, `reveladas`, `banderas`).
- No puede haber dos partidas en curso: `UsuarioRepository.bloquearPorId` usa `@Lock(PESSIMISTIC_WRITE)` (`SELECT … FOR UPDATE`) dentro de la transacción de `crear`.
- `@Version` sobre `partida.version`: dos acciones simultáneas sobre la misma partida no se pisan (409).
- Ranking: consulta nativa sobre `vw_ranking` (MIN, COUNT y AVG con GROUP BY por usuario y nivel). Los empates comparten posición.

## 8. Decisiones de diseño (diferencias con el enunciado)

1. **Victoria y derrota con el ENUM existente.** El ENUM `estado` solo admite `EN_CURSO`, `TERMINADA` y `ABANDONADA`. Una partida terminada se distingue así:
   - **Victoria**: `TERMINADA`, `tiempo_ms` con el tiempo medido en el servidor y `estrellas` de 1 a 3.
   - **Derrota**: `TERMINADA`, `tiempo_ms = NULL` y `estrellas = 0`.

   Así `vw_ranking`, que filtra `tiempo_ms IS NOT NULL`, solo cuenta victorias sin cambiar el script. La duración de las derrotas se guarda en el JSON (`duracion_ms`) y además se puede calcular con `fecha_fin - fecha_inicio`.
2. **Estrellas** (solo en victorias): Principiante ≤60 s = 3, ≤120 s = 2, más = 1. Intermedio ≤4 min / ≤8 min. Experto ≤10 min / ≤20 min.
3. **Tiempo en milisegundos.** `DATETIME` no guarda fracciones, así que el instante exacto de inicio se guarda en el JSON (`inicio_ms`) y con él se calcula `tiempo_ms`.
4. **Ruta adicional** `POST /api/partidas/{id}/abandonar`. Como crear partida responde 409 si ya hay una en curso, el jugador necesita una forma de abandonarla.
5. **Primer clic.** Las minas se colocan al crear la partida, como indica el contrato. Por eso el primer clic puede caer en una mina.
6. «Personaje inicial» en la descripción del registro no aplica al dominio Buscaminas: el registro crea solo la cuenta.
7. **CSRF desactivado.** La API es JSON del mismo origen y la cookie de sesión es `SameSite=Strict`, que el navegador no envía desde sitios de terceros.
8. El `pom.xml` está en la raíz del proyecto, según el árbol de carpetas del enunciado. Por eso `mvn spring-boot:run` se ejecuta en `examen2-respuesta/` y no en una carpeta `backend/`.
