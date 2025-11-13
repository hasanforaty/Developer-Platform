package com.devplatform.serviceregistry.controller

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: ErrorDetails? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> error(message: String, errorCode: String? = null, details: Map<String, Any>? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetails(message = message, code = errorCode, details = details)
            )
        }

        fun <T> validationError(errors: Map<String, String>): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetails(
                    message = "Validation failed",
                    code = "VALIDATION_ERROR",
                    details = errors
                )
            )
        }
    }
}

data class ErrorDetails(
    val message: String,
    val code: String? = null,
    val details: Map<String, Any>? = null
)
