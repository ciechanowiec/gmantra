package eu.ciechanowiec.gmantra;

@FunctionalInterface
interface Validator {

    ValidationResult validate();
}
