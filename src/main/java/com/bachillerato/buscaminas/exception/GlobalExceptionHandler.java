package com.bachillerato.buscaminas.exception;

import com.bachillerato.buscaminas.model.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce cualquier excepción a una respuesta JSON coherente. Los errores
 * inesperados se registran en el log del servidor pero el cliente solo recibe
 * un mensaje genérico (sin trazas ni nombres de clases o tablas).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> manejarApi(ApiException ex, HttpServletRequest req) {
        return responder(ex.getStatus(), ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex,
                                                            HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            campos.putIfAbsent(oe.getObjectName(), oe.getDefaultMessage());
        }
        HttpStatus st = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(st).body(ErrorRespuesta.conCampos(st.value(), st.getReasonPhrase(),
                "Los datos enviados no son válidos", req.getRequestURI(), campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> manejarCuerpoInvalido(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "La petición tiene un formato o un valor inválido", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> manejarMediaType(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use Content-Type: application/json", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> manejarMetodo(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido en esta ruta", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarIntegridad(DataIntegrityViolationException ex,
                                                            HttpServletRequest req) {
        log.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes", req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorRespuesta> manejarConcurrencia(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, "La partida cambió mientras se procesaba la acción; intente de nuevo", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorRespuesta> manejarAutenticacion(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> manejarAcceso(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "No tiene permiso para este recurso", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarGeneral(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", req);
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus st, String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(st)
                .body(ErrorRespuesta.de(st.value(), st.getReasonPhrase(), mensaje, req.getRequestURI()));
    }
}
