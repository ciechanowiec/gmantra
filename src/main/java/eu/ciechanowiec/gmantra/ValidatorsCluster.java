package eu.ciechanowiec.gmantra;

import eu.ciechanowiec.conditional.Conditional;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ToString
class ValidatorsCluster {

    private final Collection<Validator> validators;

    ValidatorsCluster(Repository repository, RepositoryRequirements repositoryRequirements) {
        Validator branchValidator = new BranchValidator(repository, repositoryRequirements);
        Validator messageValidator = new MessageValidator(repository, repositoryRequirements);
        validators = List.of(branchValidator, messageValidator);
        log.debug("Initialized: {}", this);
    }

    @SneakyThrows
    ValidationResult validate() {
        int numOfValidators = validators.size();
        log.debug("Started validation by {} validator(s)", numOfValidators);
        ValidationResult validationResult = validators.stream()
                .map(Validator::validate)
                .map(ValidationResult::getViolations)
                .flatMap(Collection::stream)
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableList(), ValidationResult::new));
        boolean isValid = validationResult.isOK();
        Conditional.conditional(isValid)
                   .onTrue(() -> log.info("Validation result: {}", validationResult))
                   .onFalse(() -> log.error("Validation result: {}", validationResult))
                   .execute();
        return validationResult;
    }
}
