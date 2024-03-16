package eu.ciechanowiec.gmantra;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.conditional.Conditional;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@ToString
class MessageValidator implements Validator {

    private final Repository repository;
    private final String startCommitHash;
    private final CaseConsideringPattern allowedPattern;
    private final boolean ignoreMergeCommits;

    @ToString.Exclude
    private final Function<Iterable<RevCommit>, List<RevCommit>> toList = iterable -> {
        Spliterator<RevCommit> spliterator = iterable.spliterator();
        return StreamSupport.stream(spliterator, false)
                            .collect(Collectors.toUnmodifiableList());
    };

    @ToString.Exclude
    private final Predicate<RevCommit> isNotMergeCommit = commit -> {
            int numOfParents = commit.getParentCount();
            return numOfParents < NumberUtils.INTEGER_TWO;
    };

    @SuppressWarnings("FeatureEnvy")
    MessageValidator(Repository repository, RepositoryRequirements repositoryRequirements) {
        this.repository = repository;
        this.startCommitHash = repositoryRequirements.getStartCommitHash();
        String allowedCommitMessagesRegex = repositoryRequirements.getAllowedCommitMessagesRegex();
        boolean isCaseSensitive = repositoryRequirements.isAreCaseSensitiveMatches();
        this.allowedPattern = new CaseConsideringPattern(allowedCommitMessagesRegex, isCaseSensitive);
        this.ignoreMergeCommits = repositoryRequirements.isIgnoreMergeCommits();
        log.debug("Initialized: {}", this);
    }

    @SneakyThrows
    @Override
    public ValidationResult validate() {
        log.info("Started validation by {}", this);
        try (Git git = new Git(repository)) {
            List<RevCommit> commits = ranged(git, startCommitHash, "HEAD");
            List<RevCommit> commitsWithoutMergeCommits = considerMergeCommits(commits);
            return validate(commitsWithoutMergeCommits);
        }
    }

    private ValidationResult validate(@SuppressWarnings("TypeMayBeWeakened") List<RevCommit> commits) {
        int numOfCommits = commits.size();
        log.debug("Validating this number of commits: '{}'", numOfCommits);
        ValidationResult validationResult = commits.stream()
                .map(this::validate)
                .flatMap(Optional::stream)
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableList(), ValidationResult::new));
        log.debug("Validated by {}. {}", this, validationResult);
        return validationResult;
    }

    @SuppressWarnings({"unchecked", "squid:S1612"})
    private Optional<RequirementsViolation> validate(RevCommit commit) {
        log.debug("Validating {}. [{}]", commit, readableTime(commit));
        String shortMessage = commit.getShortMessage();
        Matcher matcher = allowedPattern.matcher(shortMessage);
        boolean matches = matcher.matches();
        log.debug(
                "Does this message: '{}' from this commit: '{}' match this pattern: '{}'? Answer: '{}'",
                 shortMessage, commit, allowedPattern, matches
        );
        String violationMessage = String.format(
                "This message: '%s' from this commit: '%s' [%s] does not match this pattern: '%s'",
                 shortMessage, commit, readableTime(commit), allowedPattern
        );
        return Conditional.conditional(matches)
                          .onTrue(() -> Optional.empty())
                          .onFalse(() -> Optional.of(new RequirementsViolation(violationMessage)))
                          .get(Optional.class);
    }

    private List<RevCommit> considerMergeCommits(List<RevCommit> commits) {
        return ignoreMergeCommits ? withoutMergeCommits(commits) : commits;
    }

    @SuppressWarnings("ChainedMethodCall")
    private String readableTime(RevCommit commit) {
        int commitTime = commit.getCommitTime();
        return Instant.ofEpochSecond(commitTime)
                      .atZone(ZoneId.systemDefault())
                      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private List<RevCommit> withoutMergeCommits(@SuppressWarnings("TypeMayBeWeakened") List<RevCommit> commits) {
        log.debug("Excluding merge commits");
        int numOfCommitsBefore = commits.size();
        List<RevCommit> commitsWithoutMergeCommits = commits.stream()
                                                            .filter(isNotMergeCommit)
                                                            .collect(Collectors.toUnmodifiableList());
        int numOfCommitsAfter = commitsWithoutMergeCommits.size();
        log.debug("Number of commits before excluding merge commits: '{}'. After exclusion: '{}'",
                   numOfCommitsBefore, numOfCommitsAfter);
        return commitsWithoutMergeCommits;
    }

    @SuppressWarnings("PMD.CloseResource")
    private List<RevCommit> ranged(
            Git git, String sinceCommitHash, @SuppressWarnings("SameParameterValue") String untilCommitHash
    ) {
        log.debug("Extracting ranged commits since '{}' and until '{}'", sinceCommitHash, untilCommitHash);
        Repository gitRepository = git.getRepository();
        Optional<RevCommit> sinceNullable = parse(gitRepository, sinceCommitHash);
        Optional<RevCommit> untilNullable = parse(gitRepository, untilCommitHash);
        return sinceNullable.flatMap(
                sinceCommit -> untilNullable.map(untilCommit -> ranged(git, sinceCommit, untilCommit))
        ).orElse(
                untilNullable.map(untilCommit -> extractCommits(git, untilCommit)).orElse(List.of())
        );
    }

    @SneakyThrows
    private List<RevCommit> ranged(Git git, RevCommit since, RevCommit until) {
        log.debug("Extracting ranged commits since '{}' and until '{}'", since, until);
        LogCommand logCommand = git.log();
        // 'since' is exclusive and 'until' is inclusive:
        Iterable<RevCommit> commitsWithoutSinceAsIterable = logCommand.addRange(since, until).call();
        List<RevCommit> commitsWithoutSinceAsList = toList.apply(commitsWithoutSinceAsIterable);
        List<RevCommit> commits = Stream.concat(commitsWithoutSinceAsList.stream(), Stream.of(since))
                                        .collect(Collectors.toUnmodifiableList());
        int numOfCommits = commits.size();
        log.debug("Number of extracted commits: '{}'", numOfCommits);
        return commits;
    }

    @SneakyThrows
    private List<RevCommit> extractCommits(Git git, RevCommit until) {
        log.debug("Extracting commits until '{}'", until);
        LogCommand logCommand = git.log();
        Iterable<RevCommit> commitsIterable = logCommand.add(until).call();
        List<RevCommit> commitsList = toList.apply(commitsIterable);
        int numOfCommits = commitsList.size();
        log.debug("Number of extracted commits: '{}'", numOfCommits);
        return commitsList;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private Optional<RevCommit> parse(Repository repository, String commitHash) {
        log.debug("Parsing commit '{}' in {}", commitHash, repository);
        try {
            ObjectId commitID = repository.resolve(commitHash);
            RevCommit revCommit = repository.parseCommit(commitID);
            log.debug("Parsed commit: {}", revCommit);
            return Optional.of(revCommit);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock")
                 IOException | NullPointerException exception) {
            String message = String.format("Unable to parse this commit: '%s'", commitHash);
            boolean isEmptyCommitHash = StringUtils.isEmpty(commitHash);
            Conditional.conditional(isEmptyCommitHash)
                       .onTrue(() -> log.debug(message))
                       .onFalse(() -> log.warn(message, exception))
                       .execute();
            return Optional.empty();
        }
    }
}
