/* Historial de partidas del usuario autenticado (GET /api/usuarios/{id}/partidas). */
function PanelHistorial({ usuario }) {
    const [partidas, setPartidas] = React.useState([]);
    const [cargando, setCargando] = React.useState(true);
    const [error, setError] = React.useState('');
    const [detalle, setDetalle] = React.useState(null);

    React.useEffect(() => {
        let activo = true;
        (async () => {
            try {
                const lista = await Api.historial(usuario.id);
                if (activo) setPartidas(lista);
            } catch (err) {
                if (activo) setError(err.message);
            } finally {
                if (activo) setCargando(false);
            }
        })();
        return () => { activo = false; };
    }, [usuario.id]);

    async function verDetalle(id) {
        setError('');
        try {
            setDetalle(await Api.partida(id));
        } catch (err) {
            setError(err.message);
        }
    }

    if (cargando) return <p className="cargando">Cargando historial…</p>;

    const victorias = partidas.filter((p) => p.resultado === 'VICTORIA').length;

    return (
        <section className="tarjeta">
            <h2>Mi historial</h2>
            <p className="resumen">{partidas.length} partidas · {victorias} victorias</p>
            {error && <p className="error">{error}</p>}
            {partidas.length === 0 ? (
                <p className="vacio">Todavía no ha jugado ninguna partida.</p>
            ) : (
                <div className="tabla-scroll">
                    <table>
                        <thead>
                        <tr>
                            <th>#</th><th>Inicio</th><th>Fin</th><th>Nivel</th><th>Resultado</th>
                            <th>Duración</th><th>Mov.</th><th>Estrellas</th><th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {partidas.map((p) => (
                            <tr key={p.id} className={detalle && detalle.id === p.id ? 'seleccionada' : ''}>
                                <td>{p.id}</td>
                                <td>{Formato.fecha(p.fechaInicio)}</td>
                                <td>{Formato.fecha(p.fechaFin)}</td>
                                <td>{Formato.nivel(p.nivel)}</td>
                                <td><span className={'etiqueta ' + p.resultado.toLowerCase()}>{Formato.resultado(p.resultado)}</span></td>
                                <td>{Formato.tiempo(p.duracionMs)}</td>
                                <td>{p.movimientos}</td>
                                <td className="estrellas">{Formato.estrellas(p.estrellas)}</td>
                                <td>
                                    {p.estado !== 'EN_CURSO' &&
                                        <button className="enlace" onClick={() => verDetalle(p.id)}>Ver</button>}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
            {detalle && (
                <div className="detalle">
                    <h3>Partida #{detalle.id} · {Formato.nivel(detalle.nivel)} · {Formato.resultado(detalle.resultado)}</h3>
                    <Tablero partida={detalle} deshabilitado={true} />
                    <button className="secundario" onClick={() => setDetalle(null)}>Cerrar</button>
                </div>
            )}
        </section>
    );
}
