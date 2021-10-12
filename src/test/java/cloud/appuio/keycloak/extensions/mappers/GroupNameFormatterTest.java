package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroupNameFormatterTest {

    @Test
    void testFormat_GivenInputWithWhitespaces_ThenExpectTrim() {
        var subject = new GroupNameFormatter().withTrimWhitespace(true);

        var result = subject.format("\t  sapphire-stars\n");

        assertThat(result).isEqualTo("sapphire-stars");
    }

    @Test
    void testFormat_GivenInputWithWhitespaces_WhenContainsSpaceInTheMiddle_ThenExpectReplaceWithDashes() {
        var subject = new GroupNameFormatter().withTrimWhitespace(true);

        var result = subject.format("\t  sapphire  stars  \n");

        assertThat(result).isEqualTo("sapphire-stars");
    }

    @Test
    void testFormat_GivenInputWithWhitespaces_WhenContainsDashesInTheMiddle_ThenExpectReplaceWithSingleDashes() {
        var subject = new GroupNameFormatter().withTrimWhitespace(true);

        var result = subject.format("\t  sapphire-- -stars  \n");

        assertThat(result).isEqualTo("sapphire-stars");
    }

    @Test
    void testFormat_GivenInputWithPrefix_ThenExpectNoPrefix() {
        var subject = new GroupNameFormatter().withTrimPrefix("prefix");

        var result = subject.format("prefixsapphire-stars");

        assertThat(result).isEqualTo("sapphire-stars");
    }

    @Test
    void testFormat_GivenInputWithLowerCase_ThenExpectLowercaseName() {
        var subject = new GroupNameFormatter().withToLowerCase(true);

        var result = subject.format("SAPPHIRE StaRs");

        assertThat(result).isEqualTo("sapphire stars");
    }

    @Test
    void testFormat_GivenInputWithAllOptions_ThenExpectCorrectOrdering() {
        var subject = new GroupNameFormatter().withToLowerCase(true).withTrimPrefix("prefix").withTrimWhitespace(true);

        var result = subject.format("\t prefix SAPPHIRE     --   -         StaRs \t\n");

        assertThat(result).isEqualTo("sapphire-stars");
    }
}
