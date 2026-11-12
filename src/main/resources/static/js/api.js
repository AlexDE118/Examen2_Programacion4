/*
 * Cliente HTTP de la aplicación. Usa únicamente fetch + async/await.
 * La sesión viaja en la cookie MINASSESSION (credentials: 'same-origin').
 */

class ApiError extends Error {
    constructor(status, mensaje, campos) {
        super(mensaje);
        this.status = status;
        this.campos = campos || {};
    }
}

async function peticion(ruta, { metodo = 'GET', cuerpo } = {}) {
    const opciones = {
        method: metodo,
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' }
    };
    if (cuerpo !== undefined) {
        opciones.headers['Content-Type'] = 'application/json';
        opciones.body = JSON.stringify(cuerpo);
    }

    let respuesta;
    try {
        respuesta = await fetch(ruta, opciones);
    } catch (e) {
        throw new ApiError(0, 'No se pudo contactar al servidor. ¿Está encendido el backend?');
    }

    if (respuesta.status === 204) {
        return null;
    }

    const texto = await respuesta.text();
    let datos = null;
    if (texto) {
        try {
            datos = JSON.parse(texto);
        } catch (e) {
            datos = null;
        }
    }

    if (!respuesta.ok) {
        const mensaje = (datos && datos.mensaje) || ('Error ' + respuesta.status);
        const error = new ApiError(respuesta.status, mensaje, datos && datos.campos);
        // Sesión vencida en cualquier pantalla protegida: volver al login.
        if (respuesta.status === 401 && !ruta.startsWith('/api/auth/') && Api.onNoAutorizado) {
            Api.onNoAutorizado();
        }
        throw error;
    }
    return datos;
}

const Api = {
    onNoAutorizado: null,

    registro: (datos) => peticion('/api/auth/registro', { metodo: 'POST', cuerpo: datos }),
    login: (usuario, password) => peticion('/api/auth/login', { metodo: 'POST', cuerpo: { usuario, password } }),
    logout: () => peticion('/api/auth/logout', { metodo: 'POST' }),
    me: () => peticion('/api/auth/me'),

    niveles: () => peticion('/api/niveles'),

    crearPartida: (nivel) => peticion('/api/partidas', { metodo: 'POST', cuerpo: { nivel } }),
    partidaActual: () => peticion('/api/partidas/actual'),
    accion: (id, tipo, fila, columna) =>
        peticion('/api/partidas/' + id + '/acciones', { metodo: 'POST', cuerpo: { tipo, fila, columna } }),
    abandonar: (id) => peticion('/api/partidas/' + id + '/abandonar', { metodo: 'POST' }),
    partida: (id) => peticion('/api/partidas/' + id),

    historial: (usuarioId) => peticion('/api/usuarios/' + usuarioId + '/partidas'),
    ranking: (nivel) => peticion('/api/ranking' + (nivel ? '?nivel=' + encodeURIComponent(nivel) : ''))
};

const Formato = {
    /** 83456 -> "01:23.456" */
    tiempo(ms) {
        if (ms === null || ms === undefined) return '—';
        const total = Math.max(0, Math.floor(ms));
        const min = Math.floor(total / 60000);
        const seg = Math.floor((total % 60000) / 1000);
        const mil = total % 1000;
        return String(min).padStart(2, '0') + ':' + String(seg).padStart(2, '0') + '.' + String(mil).padStart(3, '0');
    },
    /** "2026-11-10T10:00:45" -> fecha y hora locales */
    fecha(iso) {
        if (!iso) return '—';
        const d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        return d.toLocaleString('es-CR', { dateStyle: 'short', timeStyle: 'medium' });
    },
    estrellas(n) {
        return '★'.repeat(n || 0) + '☆'.repeat(3 - (n || 0));
    },
    nivel(codigo) {
        return { PRINCIPIANTE: 'Principiante', INTERMEDIO: 'Intermedio', EXPERTO: 'Experto' }[codigo] || codigo;
    },
    resultado(r) {
        return { VICTORIA: 'Victoria', DERROTA: 'Derrota', ABANDONADA: 'Abandonada', EN_CURSO: 'En curso' }[r] || r;
    }
};
