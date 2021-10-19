package cloud.appuio.keycloak.extensions.mappers;

import org.keycloak.provider.ProviderConfigProperty;

public class GroupNameFormatter {

    private boolean trimWhitespace;
    private boolean toLowerCase;
    private String trimPrefix = "";

    GroupNameFormatter() {
    }

    String format(String input) {
        if (input == null) return "";
        var s = input;
        s = s.replaceFirst(trimPrefix, "");
        if (trimWhitespace) {
            s = s.trim().replaceAll("\\s+", "-").replaceAll("-+", "-");
        }
        if (toLowerCase) {
            s = s.toLowerCase();
        }
        return s;
    }

    GroupNameFormatter withTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
        return this;
    }

    GroupNameFormatter withToLowerCase(boolean toLowerCase) {
        this.toLowerCase = toLowerCase;
        return this;
    }

    GroupNameFormatter withTrimPrefix(String prefix) {
        this.trimPrefix = prefix == null ? "" : prefix;
        return this;
    }

    public static final String TRIM_PREFIX_PROPERTY = "trim_prefix";
    public static final String TRIM_WHITESPACE_PROPERTY = "trim_whitespace";
    public static final String TO_LOWERCASE_PROPERTY = "to_lowercase";

    public static final ProviderConfigProperty TRIM_PREFIX = new ProviderConfigProperty(
            TRIM_PREFIX_PROPERTY, "Trim Prefix",
            "Removes the first occurrence of the given regex pattern. " +
                    "Trimming the prefix occurs before trimming whitespaces (if enabled).",
            ProviderConfigProperty.STRING_TYPE, ""
    );

    public static final ProviderConfigProperty TRIM_WHITESPACE = new ProviderConfigProperty(
            TRIM_WHITESPACE_PROPERTY, "Trim whitespaces",
            "Removes leading and trailing whitespaces completely. " +
                    "Dashes and spaces between words are replaced with a single dash.",
            ProviderConfigProperty.BOOLEAN_TYPE, false
    );

    public static final ProviderConfigProperty TO_LOWERCASE = new ProviderConfigProperty(
            TO_LOWERCASE_PROPERTY, "Lowercase names",
            "Transforms the strings to lower case. ",
            ProviderConfigProperty.BOOLEAN_TYPE, false
    );
}
