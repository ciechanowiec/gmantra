package eu.ciechanowiec.gmantra;

class InvalidRepositoryException extends RuntimeException {

    InvalidRepositoryException(ValidationResult validationResult) {
        super(validationResult.toString());
    }
}
