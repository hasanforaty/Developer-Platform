package com.devplatform.serviceregistry.exception

class ServiceNotFoundException(message: String) : RuntimeException(message)

class DuplicateServiceException(message: String) : RuntimeException(message)

class InstanceNotFoundException(message: String) : RuntimeException(message)

class DuplicateInstanceException(message: String) : RuntimeException(message)

class HealthCheckException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidRequestException(message: String) : RuntimeException(message)
