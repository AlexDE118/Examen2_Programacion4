/* Ranking de mejores tiempos por nivel (GET /api/ranking), calculado desde vw_ranking. */
function TablaRanking() {
    const [ranking, setRanking] = React.useState([]);
    const [nivel, setNivel] = React.useState('PRINCIPIANTE');
    const [cargando, setCargando] = React.useState(true);
    const [error, setError] = React.useState('');

    async function cargar() {
        setCargando(true);
        setError('');
        try {
            setRanking(await Api.ranking());
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }

    React.useEffect(() => { cargar(); }, []);

    const actual = ranking.find((r) => r.nivel === nivel);

    return (
        <section className="tarjeta">
            <div className="titulo-con-accion">
                <h2>Ranking de mejores tiempos</h2>
                <button className="secundario" onClick={cargar} disabled={cargando}>Actualizar</button>
            </div>
            <div className="pestanas">
                {['PRINCIPIANTE', 'INTERMEDIO', 'EXPERTO'].map((n) => (
                    <button key={n} className={n === nivel ? 'activa' : ''} onClick={() => setNivel(n)}>
                        {Formato.nivel(n)}
                    </button>
                ))}
            </div>
            {error && <p className="error">{error}</p>}
            {cargando ? <p className="cargando">Cargando ranking…</p> : actual && (
                <>
                    <p className="mi-posicion">
                        {actual.miPosicion
                            ? <>Su posición: <strong>#{actual.miPosicion.posicion}</strong> con {Formato.tiempo(actual.miPosicion.mejorTiempoMs)}</>
                            : 'Aún no tiene victorias en este nivel.'}
                    </p>
                    {actual.posiciones.length === 0 ? (
                        <p className="vacio">Nadie ha ganado en este nivel todavía.</p>
                    ) : (
                        <div className="tabla-scroll">
                            <table>
                                <thead>
                                <tr>
                                    <th>Pos.</th><th>Jugador</th><th>Mejor tiempo</th>
                                    <th>Victorias</th><th>Prom. movimientos</th><th>Último juego</th>
                                </tr>
                                </thead>
                                <tbody>
                                {actual.posiciones.map((r) => (
                                    <tr key={r.usuarioId} className={r.esUsuarioActual ? 'yo' : ''}>
                                        <td>{r.posicion}</td>
                                        <td>{r.nombreUsuario}{r.esUsuarioActual ? ' (usted)' : ''}</td>
                                        <td className="mono">{Formato.tiempo(r.mejorTiempoMs)}</td>
                                        <td>{r.partidasTerminadas}</td>
                                        <td>{r.promedioMovimientos}</td>
                                        <td>{Formato.fecha(r.ultimoJuego)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}
        </section>
    );
}
