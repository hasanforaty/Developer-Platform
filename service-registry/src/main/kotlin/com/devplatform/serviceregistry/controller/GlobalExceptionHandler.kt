package com.devplatform.serviceregistry.controller

import com.devplatform.serviceregistry.exception.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ServiceNotFoundException::class)
    fun handleServiceNotFound(ex: ServiceNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        logger.warn("Service not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.message ?: "Service not found", "SERVICE_NOT_FOUND"))
    }

    @ExceptionHandler(DuplicateServiceException::class)
    fun handleDuplicateService(ex: DuplicateServiceException): ResponseEntity<ApiResponse<Unit>> {
        logger.warn("Duplicate service: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.message ?: "Service already exists", "DUPLICATE_SERVICE"))
    }

    @ExceptionHandler(InstanceNotFoundException::class)
    fun handleInstanceNotFound(ex: InstanceNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        logger.warn("Instance not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.message ?: "Instance not found", "INSTANCE_NOT_FOUND"))
    }

    @ExceptionHandler(DuplicateInstanceException::class)
    fun handleDuplicateInstance(ex: DuplicateInstanceException): ResponseEntity<ApiResponse<Unit>> {
        logger.warn("Duplicate instance: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.message ?: "Instance already exists", "DUPLICATE_INSTANCE"))
    }

    @ExceptionHandler(HealthCheckException::class)
    fun handleHealthCheck(ex: HealthCheckException): ResponseEntity<ApiResponse<Unit>> {
        logger.error("Health check error: ${ex.message}", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ex.message ?: "Health check failed", "HEALTH_CHECK_ERROR"))
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(ex: InvalidRequestException): ResponseEntity<ApiResponse<Unit>> {
        logger.warn("Invalid request: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.message ?: "Invalid request", "INVALID_REQUEST"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val errorMessage = error.defaultMessage ?: "Invalid value"
            fieldName to errorMessage
        }

        logger.warn("Validation errors: $errors")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.validationError(errors))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Unit>> {
        val message = "Invalid value for parameter '${ex.name}': ${ex.value}"
        logger.warn(message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message, "TYPE_MISMATCH"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"))
    }
}
