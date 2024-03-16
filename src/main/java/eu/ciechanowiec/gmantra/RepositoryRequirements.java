package eu.ciechanowiec.gmantra;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter(AccessLevel.PACKAGE)
class RepositoryRequirements {

    private final String allowedBranchesRegex;
    private final String allowedCommitMessagesRegex;
    private final boolean areCaseSensitiveMatches;
    private final String startCommitHash;
    private final boolean ignoreMergeCommits;

    @SuppressWarnings({"ParameterNumber", "ConstructorWithTooManyParameters", "PMD.ExcessiveParameterList"})
    RepositoryRequirements(
            String allowedBranchesRegex, String allowedCommitMessagesRegex, boolean areCaseSensitiveMatches,
            String startCommitHash, boolean ignoreMergeCommits
    ) {
        this.allowedBranchesRegex = allowedBranchesRegex;
        this.allowedCommitMessagesRegex = allowedCommitMessagesRegex;
        this.areCaseSensitiveMatches = areCaseSensitiveMatches;
        this.startCommitHash = startCommitHash;
        this.ignoreMergeCommits = ignoreMergeCommits;
        log.debug("Initialized: {}", this);
    }
}
