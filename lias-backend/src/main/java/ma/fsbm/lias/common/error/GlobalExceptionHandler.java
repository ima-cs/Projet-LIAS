package ma.fsbm.lias.common.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(
    ResponseStatusException exception,
    HttpServletRequest request
  ) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

    return build(status, exception.getReason(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(
    IllegalArgumentException exception,
    HttpServletRequest request
  ) {
    return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
    MethodArgumentNotValidException exception,
    HttpServletRequest request
  ) {
    String message = exception
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .findFirst()
      .map(error -> error.getField() + " : " + error.getDefaultMessage())
      .orElse("Données invalides.");

    return build(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
    EntityNotFoundException exception,
    HttpServletRequest request
  ) {
    return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleForbidden(
    AccessDeniedException exception,
    HttpServletRequest request
  ) {
    return build(HttpStatus.FORBIDDEN, "Accès interdit.", request);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(
    ConflictException exception,
    HttpServletRequest request
  ) {
    return build(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(
    org.springframework.dao.DataIntegrityViolationException.class
  )
  public ResponseEntity<ApiError> integrity(Exception e, HttpServletRequest r) {
    return build(
      HttpStatus.CONFLICT,
      "Conflit avec une donnée existante ou une contrainte métier.",
      r
    );
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.multipart.MaxUploadSizeExceededException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class,
  })
  public ResponseEntity<ApiError> malformed(Exception e, HttpServletRequest r) {
    return build(
      HttpStatus.BAD_REQUEST,
      "Données invalides ou fichier trop volumineux.",
      r
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
    Exception exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Une erreur interne est survenue.",
      request
    );
  }

  private ResponseEntity<ApiError> build(
    HttpStatus status,
    String message,
    HttpServletRequest request
  ) {
    ApiError error = new ApiError(
      Instant.now(),
      status.value(),
      status.getReasonPhrase(),
      message,
      request.getRequestURI()
    );

    return ResponseEntity.status(status).body(error);
  }
}
