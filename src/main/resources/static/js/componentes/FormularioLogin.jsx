/* Formulario de inicio de sesión. La validación real la hace el servidor. */
function FormularioLogin({ onLogin, irARegistro, aviso }) {
    const [usuario, setUsuario] = React.useState('');
    const [password, setPassword] = React.useState('');
    const [error, setError] = React.useState('');
    const [cargando, setCargando] = React.useState(false);

    async function enviar(e) {
        e.preventDefault();
        setError('');
        setCargando(true);
        try {
            const datos = await Api.login(usuario.trim(), password);
            onLogin(datos);
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }

    return (
        <div className="tarjeta formulario">
            <h2>Iniciar sesión</h2>
            {aviso && <p className="aviso">{aviso}</p>}
            <form onSubmit={enviar} noValidate>
                <label>
                    Usuario o email
                    <input value={usuario} onChange={(e) => setUsuario(e.target.value)}
                           autoComplete="username" autoFocus />
                </label>
                <label>
                    Contraseña
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                           autoComplete="current-password" />
                </label>
                {error && <p className="error">{error}</p>}
                <button type="submit" className="primario" disabled={cargando}>
                    {cargando ? 'Validando…' : 'Entrar'}
                </button>
            </form>
            <p className="pie-formulario">
                ¿No tiene cuenta? <button className="enlace" onClick={irARegistro}>Regístrese</button>
            </p>
        </div>
    );
}
