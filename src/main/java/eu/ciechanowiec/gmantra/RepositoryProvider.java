package eu.ciechanowiec.gmantra;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.function.Supplier;

import static eu.ciechanowiec.sneakyfun.SneakySupplier.sneaky;

@Slf4j
class RepositoryProvider {

    private final Supplier<Repository> lazySource;

    @SuppressWarnings("ChainedMethodCall")
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    RepositoryProvider(File codeDirectory) {
        lazySource = sneaky(
                () -> new FileRepositoryBuilder().findGitDir(codeDirectory)
                                                 .build()
        );
        log.debug("Repository provider for this directory initialized: {}", codeDirectory);
    }

    RepositoryProvider() {
        lazySource = this::fromCurrentDirectory;
        log.debug("Repository provider from the current directory initialized");
    }

    Repository get() {
        return lazySource.get();
    }

    @SneakyThrows
    @SuppressWarnings("ChainedMethodCall")
    private Repository fromCurrentDirectory() {
        log.debug("Resolving the current git repository");
        Repository repository = new FileRepositoryBuilder().readEnvironment()
                                                           .findGitDir()
                                                           .build();
        File repositoryDirectory = repository.getDirectory();
        log.info("Git directory for the current git repository: {}", repositoryDirectory);
        return repository;
    }
}
