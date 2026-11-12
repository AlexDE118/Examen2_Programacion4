/*
 * Formulario de registro. Las pistas en rojo bajo cada campo son solo una ayuda
 * visual; las reglas que cuentan son las de Bean Validation en el servidor, cuyos
 * mensajes (campo por campo) también se muestran aquí.
 */
function FormularioRegistro({ onRegistrado, irALogin }) {
    const vacio = { nombre: '', nombreUsuario: '', email: '', password: '', confirmacion: '' };
    const [datos, setDatos] = React.useState(vacio);
    const [erroresServidor, setErroresServidor] = React.useState({});
    const [error, setError] = React.useState('');
    const [cargando, setCargando] = React.useState(false);

    function cambiar(campo) {
        return (e) => setDatos({ ...datos, [campo]: e.target.value });
    }

    // Ayuda visual (no reemplaza la validación del backend).
    const pistas = {};
    if (datos.email && !/^\S+@\S+\.\S+$/.test(datos.email)) pistas.email = 'Formato de email no válido';
    if (datos.password && (datos.password.length < 8 || !/[A-Za-z]/.test(datos.password) || !/\d/.test(datos.password))) {
        pistas.password = 'Mínimo 8 caracteres, con al menos una letra y un número';
    }
    if (datos.confirmacion && datos.confirmacion !== datos.password) pistas.confirmacion = 'No coincide con la contraseña';

    async function enviar(e) {
        e.preventDefault();
        setError('');
        setErroresServidor({});
        setCargando(true);
        try {
            await Api.registro(datos);
            // Cuenta creada (201): se inicia sesión con las mismas credenciales.
            const usuario = await Api.login(datos.nombreUsuario.trim(), datos.password);
            onRegistrado(usuario);
        } catch (err) {
            const campos = { ...err.campos };
            if (campos.confirmacionValida) {
                campos.confirmacion = campos.confirmacionValida;
            }
            setErroresServidor(campos);
            setError(err.status === 409 ? err.message : (err.status === 400 ? 'Revise los campos marcados' : err.message));
        } finally {
            setCargando(false);
        }
    }

    function campo(nombre, etiqueta, tipo, autocomplete) {
        const msg = erroresServidor[nombre] || pistas[nombre];
        return (
            <label>
                {etiqueta}
                <input type={tipo || 'text'} value={datos[nombre]} onChange={cambiar(nombre)}
                       autoComplete={autocomplete} className={msg ? 'invalido' : ''} />
                {msg && <small className="error-campo">{msg}</small>}
            </label>
        );
    }

    return (
        <div className="tarjeta formulario">
            <h2>Crear cuenta</h2>
            <form onSubmit={enviar} noValidate>
                {campo('nombre', 'Nombre completo', 'text', 'name')}
                {campo('nombreUsuario', 'Nombre de usuario', 'text', 'username')}
                {campo('email', 'Email', 'email', 'email')}
                {campo('password', 'Contraseña', 'password', 'new-password')}
                {campo('confirmacion', 'Confirmar contraseña', 'password', 'new-password')}
                {error && <p className="error">{error}</p>}
                <button type="submit" className="primario" disabled={cargando}>
                    {cargando ? 'Creando cuenta…' : 'Registrarme'}
                </button>
            </form>
            <p className="pie-formulario">
                ¿Ya tiene cuenta? <button className="enlace" onClick={irALogin}>Inicie sesión</button>
            </p>
        </div>
    );
}
