package eu.ciechanowiec.gmantra;

import eu.ciechanowiec.conditional.Conditional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.Optional;

@Mojo(
        name = ValidatorMojo.GOAL_NAME, defaultPhase = LifecyclePhase.VALIDATE
)
@Slf4j
@SuppressWarnings(
        {"unused", "PMD.AvoidDuplicateLiterals", "InstanceVariableMayNotBeInitialized", "ClassWithTooManyFields"}
)
class ValidatorMojo extends AbstractMojo {

    static final String GOAL_NAME = "validate";

    /**
     * '{@code true}' if the build must fail in case of repository rule violations; '{@code false}' otherwise.
     * Although if set to '{@code false}' violations will not cause the build to fail,
     * they will still be reported in the logs as errors.
     * <br>
     * The default value is '{@code true}'.
     */
    @Parameter(property = "gmantra.failOnViolations", defaultValue = "true")
    private boolean failOnViolations;

    /**
     * A regular expression (regex) defining the allowed branch names. During the plugin's execution, it checks
     * whether the name of the current branch matches the specified regex. If there is no match, a repository
     * rule violation is reported.
     * <br>
     * The default value is <i>.*</i>, which means that by default, all branch names are allowed
     */
    @Parameter(property = "gmantra.allowedBranchesRegex", defaultValue = ".*")
    private String allowedBranchesRegex;

    /**
     * A regular expression (regex) for allowed commit messages. During the plugin's execution, it checks whether
     * the commit messages match the specified regex. If there is no match, a repository rule violation
     * is reported. Only the first line of a full commit message is verified; the rest of the message,
     * if present, is ignored. The first line is everything up to the first pair of line feeds (LFs).
     * <br>
     * The default value is '<i>.*</i>', indicating that by default, all commit messages are allowed.
     */
    @Parameter(property = "gmantra.allowedCommitMessagesRegex", defaultValue = ".*")
    private String allowedCommitMessagesRegex;

    /**
     * '{@code true}' if matches for branch names and commit messages must be performed in a case-sensitive manner;
     * '{@code false}' otherwise.
     * <br>
     * The default value is '{@code true}'.
     */
    @Parameter(property = "gmantra.isCaseSensitive", defaultValue = "true")
    private boolean areCaseSensitiveMatches;

    /**
     * <ol>
     *     <li>
     *     The hash of a Git commit that is treated as the starting point from which the checks related to
     *     commit messages will be performed. In other words, only the Git commit messages of the specified
     *     commit and all subsequent commits up to the HEAD are checked, while all commits preceding the
     *     specified commit are ignored.
     *     </li>
     *     <li>
     *     The hash can be specified in both full and abbreviated forms.
     *     </li>
     *     <li>
     *     By default, this value isn't set, and all commits are subject to validation.
     *     </li>
     *     <li>
     *     If the commit with the specified hash doesn't exist, a warning is issued, and the plugin execution
     *     proceeds as if this value wasn't specified.
     *     </li>
     * </ol>
     */
    @Parameter(property = "gmantra.startCommitHash")
    private String startCommitHash;

    /**
     * '{@code true}' if merge commits should be ignored during the validation of commit messages,
     * even if a merge commit was individually specified in the '{@code startCommitHash} setting;
     * '{@code false}' otherwise.
     * <br>
     * The default value is '{@code true}'.
     * <br>
     * This setting can be reasonable, among other reasons, when merge commits are performed in an
     * automated manner by CI/CD tools.
     */
    @Parameter(property = "gmantra.ignoreMergeCommits", defaultValue = "true")
    private boolean ignoreMergeCommits;

    private final RepositoryProvider repositoryProvider;

    ValidatorMojo() {
        repositoryProvider = new RepositoryProvider();
    }

    @SuppressWarnings({"ParameterNumber", "ConstructorWithTooManyParameters", "PMD.ExcessiveParameterList"})
    ValidatorMojo(
            File codeDirectory, boolean failOnViolations, String allowedBranchesRegex,
            String allowedCommitMessagesRegex, boolean areCaseSensitiveMatches,
            String startCommitHash, boolean ignoreMergeCommits
    ) {
        repositoryProvider = new RepositoryProvider(codeDirectory);
        this.failOnViolations = failOnViolations;
        this.allowedBranchesRegex = allowedBranchesRegex;
        this.allowedCommitMessagesRegex = allowedCommitMessagesRegex;
        this.areCaseSensitiveMatches = areCaseSensitiveMatches;
        this.startCommitHash = startCommitHash;
        this.ignoreMergeCommits = ignoreMergeCommits;
    }

    @Override
    public void execute() {
        log.info("Started plugin execution. Goal: '{}'", GOAL_NAME);
        startCommitHash = Optional.ofNullable(startCommitHash).orElse(StringUtils.EMPTY);
        RepositoryRequirements requirements = new RepositoryRequirements(
                allowedBranchesRegex, allowedCommitMessagesRegex,
                areCaseSensitiveMatches, startCommitHash, ignoreMergeCommits
        );
        log.info("Injected configuration parameters: [failOnViolations={}], [RepositoryRequirements={}]",
                  failOnViolations, requirements);
        try (Repository repository = repositoryProvider.get()) {
            ValidatorsCluster validatorsCluster = new ValidatorsCluster(repository, requirements);
            ValidationResult validationResult = validatorsCluster.validate();
            validationResult.logViolations();
            boolean isValid = validationResult.isOK();
            boolean doFail = failOnViolations && !isValid;
            Conditional.isFalseOrThrow(doFail, new InvalidRepositoryException(validationResult));
        }
    }
}
