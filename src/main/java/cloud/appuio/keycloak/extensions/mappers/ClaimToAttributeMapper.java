package cloud.appuio.keycloak.extensions.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClaimToAttributeMapper extends AbstractClaimMapper {
    private static final Logger logger = Logger.getLogger(ClaimToAttributeMapper.class);

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        this.extractClaimToAttribute(realm, mapperModel, user, new MapperConfig(mapperModel.getConfig()), context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        this.extractClaimToAttribute(realm, mapperModel, user, new MapperConfig(mapperModel.getConfig()), context);
    }

    void extractClaimToAttribute(RealmModel realm, IdentityProviderMapperModel mapperModel, UserModel user, MapperConfig config, BrokeredIdentityContext context) {
        // abort if mapper is configured incorrectly.
        if (config.getClaimName().equals("")) return;
        if (config.getTargetAttributeKey().equals("")) return;

        var claim = ClaimListExtractor.extractClaim(context, config.getClaimName());
        if (claim.isEmpty()) {
            logger.debugf(
                    "Realm [%s], IdP [%s]: no [%s] claim for user [%s], ignoring...",
                    realm.getName(), mapperModel.getIdentityProviderAlias(), config.getClaimName(), user.getUsername());
            return;
        }
        assignClaimToAttribute(realm.getName(), mapperModel.getIdentityProviderAlias(), user, claim.get(), config);
    }

    void assignClaimToAttribute(String realmName, String identityProviderAlias, UserModel user, List<String> claimEntries, MapperConfig config) {
        var attributes = user.getAttributeStream(config.getTargetAttributeKey()).collect(Collectors.toSet());
        var isAttributeAlreadyDefined = attributes.stream().anyMatch(value -> !"".equals(value));
        if (!config.enabledAttributeOverwrite() && isAttributeAlreadyDefined) {
            logger.debugf("Realm [%s], IdP [%s]: Attribute [%s] is already set for user [%s]: [%s]",
                    realmName, identityProviderAlias, config.getTargetAttributeKey(), user.getUsername(), String.join(", ", attributes)
            );
            return;
        }
        var formatter = new GroupNameFormatter()
                .withToLowerCase(config.enabledToLowerCase())
                .withTrimWhitespace(config.enabledTrimWhitespace())
                .withTrimPrefix(config.getTrimPrefix());
        var filteredGroups = claimEntries.stream()
                .filter(this::ignoreEmptyEntries)
                .filter(group -> ignoreEntriesThatMatchRegex(config, group))
                .filter(group -> includeEntriesThatMatchRegex(config, group))
                .map(formatter::format)
                .collect(Collectors.toList());

        if (filteredGroups.size() != 1) {
            if (!isAttributeAlreadyDefined) {
                logger.infof("Realm [%s], IdP [%s]: Cannot reduce claim entries list to one entry for [%s]. Claim has following entries after reduction: [%s].",
                        realmName, identityProviderAlias, user.getUsername(), String.join(", ", filteredGroups));
            }
            return;
        }
        var groupName = filteredGroups.get(0);
        user.setAttribute(config.getTargetAttributeKey(), List.of(groupName));
        logger.debugf("Realm [%s], IdP [%s]: Set the attribute [%s] for [%s] to [%s].",
                realmName, identityProviderAlias, config.getTargetAttributeKey(), user.getUsername(), groupName);
    }

    private boolean ignoreEntriesThatMatchRegex(MapperConfig config, String groupModel) {
        return !groupModel.matches(config.getIgnoreEntriesPattern());
    }

    private boolean includeEntriesThatMatchRegex(MapperConfig config, String groupModel) {
        return "".equals(config.getSearchEntriesPattern()) || groupModel.matches(config.getSearchEntriesPattern());
    }

    private boolean ignoreEmptyEntries(String group) {
        return !"".equals(group);
    }

    // Boilerplate

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Claim to Attribute";
    }

    @Override
    public String getHelpText() {
        return "Extracts a claim from a user and updates a user's attribute. " +
                "If the claim is a list of strings it tries to reduce entries to one entry only. " +
                "However it does nothing if there are zero or multiple entries. " +
                "Use the ignore entries config option to exclude irrelevant entries.";
    }

    public static final String IGNORE_ENTRIES_PROPERTY = "ignore_entries";
    public static final String SEARCH_ENTRIES_PROPERTY = "search_entries";
    public static final String TARGET_ATTRIBUTE_PROPERTY = "target_attribute";
    public static final String OVERWRITE_ATTRIBUTE_PROPERTY = "overwrite_attribute";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        var ignoreEntries = new ProviderConfigProperty(
                IGNORE_ENTRIES_PROPERTY, "Ignore entries pattern", null, ProviderConfigProperty.STRING_TYPE, null
        );
        ignoreEntries.setHelpText("The claim might contain multiple values that you want to ignore. " +
                "This pattern is a regex that is matched against each claim entry before being trimmed or formatted. " +
                "If the pattern matches, the entry is ignored.");

        var searchEntries = new ProviderConfigProperty(
                SEARCH_ENTRIES_PROPERTY, "Search entries pattern", null, ProviderConfigProperty.STRING_TYPE, null
        );
        searchEntries.setHelpText("You might be only interested in certain values of a claim. " +
                "This pattern is a regex that is matched against each claim entry before being trimmed or formatted. " +
                "Only entries that match this pattern are further processed. " +
                "'Ignore entries pattern' takes precedence and is basically the inverse behavior of this setting.");

        var targetAttribute = new ProviderConfigProperty(
                TARGET_ATTRIBUTE_PROPERTY, "Target attribute", null, ProviderConfigProperty.STRING_TYPE, ""
        );
        targetAttribute.setHelpText("**REQUIRED** The user attribute key which the mapper should update. " +
                "If the attribute is already present but with an empty string, it will be updated.");

        var overwriteAttribute = new ProviderConfigProperty(
                OVERWRITE_ATTRIBUTE_PROPERTY, "Overwrite existing attribute value", null, ProviderConfigProperty.BOOLEAN_TYPE, false
        );
        overwriteAttribute.setHelpText("Overwrite the value of the target attribute even if it is already set.");

        var claimProperty = new ProviderConfigProperty(
                CLAIM, "Claim name", null, ProviderConfigProperty.STRING_TYPE, ""
        );
        claimProperty.setHelpText("Name of the claim to search for in token. " +
                "You can reference nested claims using a '.', i.e. 'address.locality'. " +
                "To use dot (.) literally, escape it with backslash (\\.)"
        );

        return List.of(claimProperty, targetAttribute, overwriteAttribute, ignoreEntries, searchEntries, GroupNameFormatter.TO_LOWERCASE, GroupNameFormatter.TRIM_WHITESPACE, GroupNameFormatter.TRIM_PREFIX);
    }

    static class MapperConfig {
        Map<String, String> map;

        MapperConfig(Map<String, String> map) {
            this.map = map;
        }

        String getIgnoreEntriesPattern() {
            return map.getOrDefault(IGNORE_ENTRIES_PROPERTY, "");
        }

        String getSearchEntriesPattern() {
            return map.getOrDefault(SEARCH_ENTRIES_PROPERTY, "");
        }

        String getTargetAttributeKey() {
            return map.getOrDefault(TARGET_ATTRIBUTE_PROPERTY, "");
        }

        String getTrimPrefix() {
            return map.getOrDefault(GroupNameFormatter.TRIM_PREFIX_PROPERTY, "");
        }

        boolean enabledTrimWhitespace() {
            return Boolean.parseBoolean(map.getOrDefault(GroupNameFormatter.TRIM_WHITESPACE_PROPERTY, String.valueOf(false)));
        }

        boolean enabledToLowerCase() {
            return Boolean.parseBoolean(map.getOrDefault(GroupNameFormatter.TO_LOWERCASE_PROPERTY, String.valueOf(false)));
        }

        String getClaimName() {
            return map.getOrDefault(CLAIM, "");
        }

        boolean enabledAttributeOverwrite() {
            return Boolean.parseBoolean(map.getOrDefault(OVERWRITE_ATTRIBUTE_PROPERTY, String.valueOf(false)));
        }
    }

    @Override
    public String[] getCompatibleProviders() {
        return new String[]{
                KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
                OIDCIdentityProviderFactory.PROVIDER_ID
        };
    }

    @Override
    public String getId() {
        return "claim-to-attribute-mapper";
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return Arrays.asList(IdentityProviderSyncMode.IMPORT, IdentityProviderSyncMode.FORCE).contains(syncMode);
    }
}
