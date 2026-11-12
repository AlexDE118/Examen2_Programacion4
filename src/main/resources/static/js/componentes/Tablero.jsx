/*
 * Tablero de juego. El navegador nunca conoce las minas: solo dibuja las celdas
 * filtradas que devuelve el servidor y le envía cada clic.
 *   "O" oculta · "B" bandera · "0".."8" abierta · "M" mina · "X" mina pisada · "BI" bandera errónea
 */
function Casilla({ valor, fila, columna, onRevelar, onBandera, deshabilitada }) {
    let clase = 'casilla';
    let contenido = '';
    if (valor === 'O') {
        clase += ' oculta';
    } else if (valor === 'B') {
        clase += ' oculta bandera';
        contenido = '🚩';
    } else if (valor === 'M') {
        clase += ' abierta mina';
        contenido = '💣';
    } else if (valor === 'X') {
        clase += ' abierta mina explotada';
        contenido = '💥';
    } else if (valor === 'BI') {
        clase += ' abierta bandera-incorrecta';
        contenido = '❌';
    } else if (valor === '0') {
        clase += ' abierta vacia';
    } else {
        clase += ' abierta numero n' + valor;
        contenido = valor;
    }

    const interactiva = !deshabilitada && (valor === 'O' || valor === 'B');

    return (
        <button
            type="button"
            className={clase}
            disabled={!interactiva}
            aria-label={'Fila ' + (fila + 1) + ', columna ' + (columna + 1)}
            onClick={() => interactiva && valor === 'O' && onRevelar(fila, columna)}
            onContextMenu={(e) => {
                e.preventDefault();
                if (interactiva) onBandera(fila, columna);
            }}>
            {contenido}
        </button>
    );
}

function Tablero({ partida, onRevelar, onBandera, deshabilitado }) {
    const estilo = { gridTemplateColumns: 'repeat(' + partida.columnas + ', var(--tam-casilla))' };
    return (
        <div className="tablero-scroll">
            <div className="tablero" style={estilo} onContextMenu={(e) => e.preventDefault()}>
                {partida.celdas.map((fila, f) =>
                    fila.map((valor, c) => (
                        <Casilla key={f + '-' + c} valor={valor} fila={f} columna={c}
                                 onRevelar={onRevelar || (() => {})} onBandera={onBandera || (() => {})}
                                 deshabilitada={deshabilitado || partida.estado !== 'EN_CURSO'} />
                    ))
                )}
            </div>
        </div>
    );
}

/* Vista de juego: elegir nivel, crear/continuar partida, jugar y abandonar. */
function VistaJuego() {
    const [niveles, setNiveles] = React.useState([]);
    const [nivel, setNivel] = React.useState('PRINCIPIANTE');
    const [partida, setPartida] = React.useState(null);
    const [cargando, setCargando] = React.useState(true);
    const [ocupado, setOcupado] = React.useState(false);
    const [error, setError] = React.useState('');
    const [recibidoEn, setRecibidoEn] = React.useState(Date.now());
    const [ahora, setAhora] = React.useState(Date.now());

    function mostrar(p) {
        setPartida(p);
        setRecibidoEn(Date.now());
        setAhora(Date.now());
    }

    React.useEffect(() => {
        let activo = true;
        async function cargar() {
            try {
                const lista = await Api.niveles();
                if (activo) setNiveles(lista);
                try {
                    const actual = await Api.partidaActual();
                    if (activo) {
                        mostrar(actual);
                        setNivel(actual.nivel);
                    }
                } catch (err) {
                    if (err.status !== 404) throw err; // 404 = no hay partida en curso
                }
            } catch (err) {
                if (activo) setError(err.message);
            } finally {
                if (activo) setCargando(false);
            }
        }
        cargar();
        return () => { activo = false; };
    }, []);

    // Reloj visual: parte del tiempo que calculó el servidor.
    React.useEffect(() => {
        if (!partida || partida.estado !== 'EN_CURSO') return undefined;
        const id = setInterval(() => setAhora(Date.now()), 200);
        return () => clearInterval(id);
    }, [partida]);

    async function nuevaPartida() {
        setError('');
        setOcupado(true);
        try {
            mostrar(await Api.crearPartida(nivel));
        } catch (err) {
            setError(err.message);
            if (err.status === 409) {
                try { mostrar(await Api.partidaActual()); } catch (e) { /* sin partida */ }
            }
        } finally {
            setOcupado(false);
        }
    }

    async function enviarAccion(tipo, fila, columna) {
        if (ocupado || !partida) return;
        setError('');
        setOcupado(true);
        try {
            mostrar(await Api.accion(partida.id, tipo, fila, columna));
        } catch (err) {
            setError(err.message);
        } finally {
            setOcupado(false);
        }
    }

    async function abandonar() {
        if (!partida || !window.confirm('¿Abandonar la partida? Quedará registrada como abandonada.')) return;
        setOcupado(true);
        setError('');
        try {
            mostrar(await Api.abandonar(partida.id));
        } catch (err) {
            setError(err.message);
        } finally {
            setOcupado(false);
        }
    }

    if (cargando) return <p className="cargando">Cargando partida…</p>;

    const enCurso = partida && partida.estado === 'EN_CURSO';
    const tiempo = !partida ? 0
        : enCurso ? partida.transcurridoMs + (ahora - recibidoEn)
        : (partida.tiempoMs !== null ? partida.tiempoMs : partida.transcurridoMs);

    return (
        <section className="vista-juego">
            <div className="barra-juego tarjeta">
                <label className="selector-nivel">
                    Nivel
                    <select value={nivel} onChange={(e) => setNivel(e.target.value)} disabled={enCurso || ocupado}>
                        {niveles.map((n) => (
                            <option key={n.codigo} value={n.codigo}>
                                {n.nombre} · {n.filas}×{n.columnas} · {n.minas} minas
                            </option>
                        ))}
                    </select>
                </label>
                {enCurso
                    ? <button className="secundario" onClick={abandonar} disabled={ocupado}>Abandonar</button>
                    : <button className="primario" onClick={nuevaPartida} disabled={ocupado}>Nueva partida</button>}
            </div>

            {error && <p className="error">{error}</p>}

            {!partida && <p className="vacio">Elija un nivel y presione «Nueva partida».</p>}

            {partida && (
                <div className="tarjeta zona-tablero">
                    <div className="marcador">
                        <span title="Minas restantes">
                            💣 {partida.resultado === 'VICTORIA' ? 0 : partida.totalMinas - partida.banderas}
                        </span>
                        <span className={'estado-partida ' + partida.resultado.toLowerCase()}>
                            {partida.resultado === 'VICTORIA' && '¡Victoria! ' + Formato.estrellas(partida.estrellas)}
                            {partida.resultado === 'DERROTA' && 'Derrota: pisó una mina'}
                            {partida.resultado === 'ABANDONADA' && 'Partida abandonada'}
                            {partida.resultado === 'EN_CURSO' && Formato.nivel(partida.nivel)}
                        </span>
                        <span title="Tiempo medido en el servidor">⏱ {Formato.tiempo(tiempo)}</span>
                    </div>
                    <Tablero partida={partida} deshabilitado={ocupado}
                             onRevelar={(f, c) => enviarAccion('REVELAR', f, c)}
                             onBandera={(f, c) => enviarAccion('BANDERA', f, c)} />
                    <p className="ayuda">Clic izquierdo: descubrir · Clic derecho: poner o quitar bandera · Movimientos: {partida.movimientos}</p>
                </div>
            )}
        </section>
    );
}
