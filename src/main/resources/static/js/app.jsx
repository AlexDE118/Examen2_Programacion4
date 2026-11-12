/*
 * Raíz de la aplicación. La navegación se resuelve con renderizado condicional
 * a partir de la variable de estado «vista» (sin librería de enrutado).
 * Las vistas de juego, historial y ranking solo existen si hay «usuario».
 */
function App() {
    const [usuario, setUsuario] = React.useState(null);
    const [verificando, setVerificando] = React.useState(true);
    const [vista, setVista] = React.useState('login');
    const [aviso, setAviso] = React.useState('');

    React.useEffect(() => {
        Api.onNoAutorizado = () => {
            setUsuario(null);
            setVista('login');
            setAviso('Su sesión terminó. Inicie sesión de nuevo.');
        };
        (async () => {
            try {
                const yo = await Api.me();
                setUsuario(yo);
                setVista('juego');
            } catch (e) {
                setUsuario(null); // 401: no hay sesión
            } finally {
                setVerificando(false);
            }
        })();
    }, []);

    function entrar(u) {
        setAviso('');
        setUsuario(u);
        setVista('juego');
    }

    async function salir() {
        try {
            await Api.logout();
        } catch (e) {
            /* si la sesión ya no existía, igual se limpia el estado */
        }
        setUsuario(null);
        setVista('login');
    }

    if (verificando) return <p className="cargando-inicial">Cargando…</p>;

    let contenido;
    if (!usuario) {
        contenido = vista === 'registro'
            ? <FormularioRegistro onRegistrado={entrar} irALogin={() => setVista('login')} />
            : <FormularioLogin onLogin={entrar} irARegistro={() => { setAviso(''); setVista('registro'); }} aviso={aviso} />;
    } else if (vista === 'historial') {
        contenido = <PanelHistorial usuario={usuario} />;
    } else if (vista === 'ranking') {
        contenido = <TablaRanking />;
    } else {
        contenido = <VistaJuego />;
    }

    return (
        <div className="app">
            <header className="cabecera">
                <h1>💣 Minas</h1>
                {usuario && (
                    <nav>
                        {[['juego', 'Jugar'], ['historial', 'Historial'], ['ranking', 'Ranking']].map(([clave, texto]) => (
                            <button key={clave} className={vista === clave ? 'activa' : ''} onClick={() => setVista(clave)}>
                                {texto}
                            </button>
                        ))}
                    </nav>
                )}
                {usuario && (
                    <div className="sesion">
                        <span>{usuario.nombre} <small>@{usuario.nombreUsuario}</small></span>
                        <button className="secundario" onClick={salir}>Salir</button>
                    </div>
                )}
            </header>
            <main>{contenido}</main>
        </div>
    );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
