package eu.ciechanowiec.gmantra;

import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class ConsolidatedTest {

    private File codeDirectory;

    @BeforeEach
    @SneakyThrows
    void setup() {
        codeDirectory = extractCodeDirectory();
    }

    @Test
    void testMojoWithFail() {
        ValidatorMojo validatorMojo = new ValidatorMojo(
                codeDirectory, true, "branchus", "messagus.*", true, "", true
        );
        assertThrows(InvalidRepositoryException.class, validatorMojo::execute);
    }

    @Test
    void testMojoWithoutFail() {
        ValidatorMojo validatorMojo = new ValidatorMojo(
                codeDirectory, false, "branchus", "messagus.*", true, "", true
        );
        assertDoesNotThrow(validatorMojo::execute);
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    @SuppressWarnings(
            {"ParameterNumber", "MethodWithTooManyParameters", "PMD.ExcessiveParameterList", "PMD.CloseResource"}
    )
    void testBasicLogic(
            String allowedBranchesRegex, String allowedCommitMessagesRegex, boolean areCaseSensitiveMatches,
            String startCommitHash, boolean ignoreMergeCommits, String expectedResult
    ) {
        RepositoryProvider repositoryProvider = new RepositoryProvider(codeDirectory);
        Repository repository = repositoryProvider.get();
        RepositoryRequirements repositoryRequirements = new RepositoryRequirements(
                allowedBranchesRegex, allowedCommitMessagesRegex, areCaseSensitiveMatches,
                startCommitHash, ignoreMergeCommits
        );
        ValidatorsCluster validatorsCluster = new ValidatorsCluster(repository, repositoryRequirements);
        ValidationResult validationResult = validatorsCluster.validate();
        String actualResult = validationResult.toString();
        assertEquals(expectedResult, actualResult);
    }

    @SuppressWarnings({"LineLength", "MethodLength"})
    static Stream<Arguments> testArgs() {
        // allowedBranchesRegex, allowedCommitMessagesRegex, areCaseSensitiveMatches, startCommitHash, ignoreMergeCommits, expectedResult
        return Stream.of(
                arguments(
                        // ok
                        "main", "messagus.*", true, "", true, "ValidationResult(violations=[], isOK=true)"
                ),
                arguments(
                        // invalidBranchName
                        "branchus", "messagus.*", true, "", true, "ValidationResult(violations=[RequirementsViolation(message=This branch name: 'main' does not match this pattern: 'branchus')], isOK=false)"
                ),
                arguments(
                        // invalidMessage
                        "main", "dalidorka.*", true, "", true, "ValidationResult(violations=[RequirementsViolation(message=This message: 'messagus-5' from this commit: 'commit 9db09a021fc8873ad972140196d21465e6adfc07 1710517172 -----sp' [2024-03-15 16:39:32] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-4' from this commit: 'commit 03f9cd4759103e3e41424e502e9ae1f02f46ae1b 1710517104 -----sp' [2024-03-15 16:38:24] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-3' from this commit: 'commit 893d15b30068a172471079f9584cbe634e7b10e5 1710517062 -----sp' [2024-03-15 16:37:42] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-2' from this commit: 'commit d61ea0bb886d829cd462932940af06461988ddd5 1710516999 -----sp' [2024-03-15 16:36:39] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-1' from this commit: 'commit 46a8a54d66d624f4da7d663db41e9788b023b848 1710516935 -----sp' [2024-03-15 16:35:35] does not match this pattern: 'dalidorka.*')], isOK=false)"
                ),
                arguments(
                        // invalidBranchNameAndMessage
                        "branchus", "dalidorka.*", true, "", true, "ValidationResult(violations=[RequirementsViolation(message=This branch name: 'main' does not match this pattern: 'branchus'), RequirementsViolation(message=This message: 'messagus-5' from this commit: 'commit 9db09a021fc8873ad972140196d21465e6adfc07 1710517172 -----sp' [2024-03-15 16:39:32] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-4' from this commit: 'commit 03f9cd4759103e3e41424e502e9ae1f02f46ae1b 1710517104 -----sp' [2024-03-15 16:38:24] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-3' from this commit: 'commit 893d15b30068a172471079f9584cbe634e7b10e5 1710517062 -----sp' [2024-03-15 16:37:42] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-2' from this commit: 'commit d61ea0bb886d829cd462932940af06461988ddd5 1710516999 -----sp' [2024-03-15 16:36:39] does not match this pattern: 'dalidorka.*'), RequirementsViolation(message=This message: 'messagus-1' from this commit: 'commit 46a8a54d66d624f4da7d663db41e9788b023b848 1710516935 -----sp' [2024-03-15 16:35:35] does not match this pattern: 'dalidorka.*')], isOK=false)"
                ),
                arguments(
                        // passBranchWithDifferentCase
                        "MAIN", "messagus.*", false, "", true, "ValidationResult(violations=[], isOK=true)"
                ),
                arguments(
                        // failBranchWithDifferentCase
                        "MAIN", "messagus.*", true, "", true, "ValidationResult(violations=[RequirementsViolation(message=This branch name: 'main' does not match this pattern: 'MAIN')], isOK=false)"
                ),
                arguments(
                        // passMessageWithDifferentCase
                        "main", "MESSAGUS.*", false, "", true, "ValidationResult(violations=[], isOK=true)"
                ),
                arguments(
                        // failMessageWithDifferentCase
                        "main", "MESSAGUS.*", true, "", true, "ValidationResult(violations=[RequirementsViolation(message=This message: 'messagus-5' from this commit: 'commit 9db09a021fc8873ad972140196d21465e6adfc07 1710517172 -----sp' [2024-03-15 16:39:32] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-4' from this commit: 'commit 03f9cd4759103e3e41424e502e9ae1f02f46ae1b 1710517104 -----sp' [2024-03-15 16:38:24] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-3' from this commit: 'commit 893d15b30068a172471079f9584cbe634e7b10e5 1710517062 -----sp' [2024-03-15 16:37:42] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-2' from this commit: 'commit d61ea0bb886d829cd462932940af06461988ddd5 1710516999 -----sp' [2024-03-15 16:36:39] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-1' from this commit: 'commit 46a8a54d66d624f4da7d663db41e9788b023b848 1710516935 -----sp' [2024-03-15 16:35:35] does not match this pattern: 'MESSAGUS.*')], isOK=false)"
                ),
                arguments(
                        // passBranchAndMessagesWithDifferentCase
                        "MAIN", "MESSAGUS.*", false, "", true, "ValidationResult(violations=[], isOK=true)"
                ),
                arguments(
                        // failsBranchAndMessagesWithDifferentCase
                        "MAIN", "MESSAGUS.*", true, "", true, "ValidationResult(violations=[RequirementsViolation(message=This branch name: 'main' does not match this pattern: 'MAIN'), RequirementsViolation(message=This message: 'messagus-5' from this commit: 'commit 9db09a021fc8873ad972140196d21465e6adfc07 1710517172 -----sp' [2024-03-15 16:39:32] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-4' from this commit: 'commit 03f9cd4759103e3e41424e502e9ae1f02f46ae1b 1710517104 -----sp' [2024-03-15 16:38:24] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-3' from this commit: 'commit 893d15b30068a172471079f9584cbe634e7b10e5 1710517062 -----sp' [2024-03-15 16:37:42] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-2' from this commit: 'commit d61ea0bb886d829cd462932940af06461988ddd5 1710516999 -----sp' [2024-03-15 16:36:39] does not match this pattern: 'MESSAGUS.*'), RequirementsViolation(message=This message: 'messagus-1' from this commit: 'commit 46a8a54d66d624f4da7d663db41e9788b023b848 1710516935 -----sp' [2024-03-15 16:35:35] does not match this pattern: 'MESSAGUS.*')], isOK=false)"
                ),
                arguments(
                        // passWithMergeCommit
                        "main", "messagus.*|Merge commit", true, "", false, "ValidationResult(violations=[], isOK=true)"
                ),
                arguments(
                        // failWithMergeCommit
                        "main", "messagus.*", true, "", false, "ValidationResult(violations=[RequirementsViolation(message=This message: 'Merge commit' from this commit: 'commit cf8fedf99edd1e487925ce9da87c7c7def7af5ba 1710517149 -----sp' [2024-03-15 16:39:09] does not match this pattern: 'messagus.*')], isOK=false)"
                ),
                arguments(
                        // passWithStartCommit
                        "main", "messagus-[2-5]", true, "d61ea0bb", true, "ValidationResult(violations=[], isOK=true)"
                ),
                arguments(
                        // failWithStartCommit
                        "main", "messagus-[3-5]", true, "d61ea0bb", true, "ValidationResult(violations=[RequirementsViolation(message=This message: 'messagus-2' from this commit: 'commit d61ea0bb886d829cd462932940af06461988ddd5 1710516999 ------p' [2024-03-15 16:36:39] does not match this pattern: 'messagus-[3-5]')], isOK=false)"
                ),
                arguments(
                        // failNonExistentStartCommitWithCheckingTheWholeTree
                        "main", "messagus-[3-5]", true, "non-existent-hash", true, "ValidationResult(violations=[RequirementsViolation(message=This message: 'messagus-2' from this commit: 'commit d61ea0bb886d829cd462932940af06461988ddd5 1710516999 -----sp' [2024-03-15 16:36:39] does not match this pattern: 'messagus-[3-5]'), RequirementsViolation(message=This message: 'messagus-1' from this commit: 'commit 46a8a54d66d624f4da7d663db41e9788b023b848 1710516935 -----sp' [2024-03-15 16:35:35] does not match this pattern: 'messagus-[3-5]')], isOK=false)"
                )
        );
    }

    @SneakyThrows
    private File extractCodeDirectory() {
        ClassLoader classLoader = ConsolidatedTest.class.getClassLoader();
        URL zipURL = classLoader.getResource("repo-1.zip");
        Objects.requireNonNull(zipURL);
        URI zipURI = zipURL.toURI();
        File zip = new File(zipURI);
        Path zipPath = zip.toPath();
        try (
                ZipInputStream zipIS = new ZipInputStream(Files.newInputStream(zipPath));
                ZipFile zipFile = new ZipFile(zip)
        ) {
            ZipEntry rootDirectory = Optional.ofNullable(zipIS.getNextEntry()).orElseThrow();
            String rootDirectoryName = rootDirectory.getName();
            //HERE CONTINUE:
            Path unzippedPath = Files.createTempDirectory("project_unzipped_");
            String unzippedPathAsString = unzippedPath.toString();
            zipFile.extractFile(rootDirectoryName, unzippedPathAsString);
            URI unzippedURI = unzippedPath.toUri();
            Path unzippedRootDirectoryPath = Path.of(unzippedURI).resolve(rootDirectoryName);
            URI unzippedRootDirectoryURI = unzippedRootDirectoryPath.toUri();
            File unzipped = new File(unzippedRootDirectoryURI);
            unzipped.deleteOnExit();
            return unzipped;
        }
    }
}
