package eu.ciechanowiec.gmantra;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter
@ToString
class ValidationResult {

    private final Collection<RequirementsViolation> violations;
    private final boolean isOK;

    ValidationResult(RequirementsViolation... violations) {
        this.violations = Stream.of(violations).collect(Collectors.toUnmodifiableList());
        this.isOK = this.violations.isEmpty();
        log.debug("Initialized: {}", this);
    }

    ValidationResult(Collection<RequirementsViolation> violations) {
        this.violations = List.copyOf(violations);
        this.isOK = this.violations.isEmpty();
        log.debug("Initialized: {}", this);
    }

    void logViolations() {
        violations.stream()
                  .map(RequirementsViolation::getMessage)
                  .forEach(log::error);
    }
}
