package eu.ciechanowiec.gmantra;

import eu.ciechanowiec.conditional.Conditional;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class CaseConsideringPattern {

    private final Pattern origin;

    CaseConsideringPattern(String regex, boolean isCaseSensitive) {
        this.origin = generate(regex, isCaseSensitive);
    }

    Matcher matcher(CharSequence input) {
        return origin.matcher(input);
    }

    private Pattern generate(String regex, boolean isCaseSensitive) {
        log.debug("Creating a Pattern for '{}' regex. Is case sensitive: '{}'", regex, isCaseSensitive);
        Pattern pattern = Conditional.conditional(isCaseSensitive)
                                     .onTrue(() -> Pattern.compile(regex))
                                     .onFalse(() -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                                     .get(Pattern.class);
        log.debug("Pattern created: '{}'", pattern);
        return pattern;
    }

    @Override
    public String toString() {
        return origin.toString();
    }
}
