package eu.ciechanowiec.gmantra;

import eu.ciechanowiec.conditional.Conditional;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Repository;

import java.util.regex.Matcher;

@Slf4j
@ToString
class BranchValidator implements Validator {

    private final Repository repository;
    private final CaseConsideringPattern allowedPattern;

    BranchValidator(Repository repository, RepositoryRequirements repositoryRequirements) {
        this.repository = repository;
        String allowedBranchesRegex = repositoryRequirements.getAllowedBranchesRegex();
        boolean isCaseSensitive = repositoryRequirements.isAreCaseSensitiveMatches();
        this.allowedPattern = new CaseConsideringPattern(allowedBranchesRegex, isCaseSensitive);
        log.debug("Initialized: {}", this);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("squid:S1612")
    public ValidationResult validate() {
        log.info("Started validation by {}", this);
        String currentBranch = repository.getBranch();
        log.debug("Current branch name: '{}'", currentBranch);
        Matcher matcher = allowedPattern.matcher(currentBranch);
        boolean doesMatch = matcher.matches();
        log.info("Does this branch name: '{}' match this pattern: '{}'? Answer: '{}'",
                  currentBranch, allowedPattern, doesMatch);
        String violationMessage = String.format(
                "This branch name: '%s' does not match this pattern: '%s'", currentBranch, allowedPattern
        );
        ValidationResult validationResult = Conditional.conditional(doesMatch)
                .onTrue(() -> new ValidationResult())
                .onFalse(() -> new ValidationResult(new RequirementsViolation(violationMessage)))
                .get(ValidationResult.class);
        log.debug("Validated by {}. {}", this, validationResult);
        return validationResult;
    }
}
