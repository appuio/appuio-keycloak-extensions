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
import java.util.stream.Stream;

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
        // check if mapper is configured correctly.
        if (config.getClaimName().equals("")) return;
        if (config.getTargetAttributeKey().equals("")) return;

        var claim = ClaimListExtractor.extractClaim(context, config.getClaimName());
        if (claim.isEmpty()) {
            logger.debugf(
                    "Realm [%s], IdP [%s]: no [%s] claim for user [%s], ignoring...",
                    realm.getName(),
                    mapperModel.getIdentityProviderAlias(),
                    config.getClaimName(),
                    user.getUsername());
            return;
        }
        assignClaimToAttribute(realm.getName(), mapperModel.getIdentityProviderAlias(), user, claim.get(), config);
    }

    void assignClaimToAttribute(String realmName, String identityProviderAlias, UserModel user, List<String> claimEntries, MapperConfig config) {
        var attributes = user.getAttributeStream(config.getTargetAttributeKey()).collect(Collectors.toSet());
        if (!config.enabledAttributeOverwrite()) {
            var isAlreadyDefined = attributes.stream().anyMatch(value -> !"".equals(value));
            if (isAlreadyDefined) {
                logger.debugf("Realm [%s], IdP [%s]: Attribute [%s] is already set for user [%s]: [%s]",
                        realmName,
                        identityProviderAlias,
                        config.getTargetAttributeKey(),
                        user.getUsername(),
                        String.join(", ", attributes)
                );
                return;
            }
        }
        var formatter = new GroupNameFormatter()
                .withToLowerCase(config.enabledToLowerCase())
                .withTrimWhitespace(config.enabledTrimWhitespace())
                .withTrimPrefix(config.getTrimPrefix());
        var filteredGroups = claimEntries.stream()
                .filter(this::ignoreEmptyEntries)
                .filter(group -> ignoreEntriesThatMatchRegex(config, group))
                .map(formatter::format)
                .collect(Collectors.toList());

        if (filteredGroups.size() != 1) {
            logger.warnf("Realm [%s], IdP [%s]: Cannot reduce claim entries list to one entry for [%s]. Claim has following entries after reduction: [%s].",
                    user.getUsername(),
                    String.join(", ", filteredGroups));
            return;
        }
        var groupName = filteredGroups.get(0);
        user.setAttribute(config.getTargetAttributeKey(), List.of(groupName));
        logger.infof("Realm [%s], IdP [%s]: Set the attribute [%s] for [%s] to [%s].", config.getTargetAttributeKey(), user.getUsername(), groupName);
    }

    private boolean ignoreEntriesThatMatchRegex(MapperConfig config, String groupModel) {
        return config.getIgnoreEntriesPatterns().noneMatch(groupModel::matches);
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
    public static final String TARGET_ATTRIBUTE_PROPERTY = "target_attribute";
    public static final String OVERWRITE_ATTRIBUTE_PROPERTY = "overwrite_attribute";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        var ignoreEntries = new ProviderConfigProperty(
                IGNORE_ENTRIES_PROPERTY, "Ignore entries patterns", null, ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null
        );
        ignoreEntries.setHelpText("The claim might contain multiple values that you want to ignore. " +
                "Each pattern is a regex that is matched against each claim entry before being trimmed or formatted. " +
                "If any pattern matches, the entry is ignored. " +
                "NOTE: Do NOT specify 2 or more '#' in sequence per pattern.");

        var targetAttribute = new ProviderConfigProperty(
                TARGET_ATTRIBUTE_PROPERTY, "Target attribute", null, ProviderConfigProperty.STRING_TYPE, ""
        );
        targetAttribute.setHelpText("**REQUIRED** The user attribute key which the mapper should update. " +
                "If this attribute is already set with a non-empty string, it will not be updated.");

        var overwriteAttribute = new ProviderConfigProperty(
                OVERWRITE_ATTRIBUTE_PROPERTY, "Overwrite existing attribute value", null, ProviderConfigProperty.BOOLEAN_TYPE, false
        );
        overwriteAttribute.setHelpText("Overwrite the value of the target attribute even if it is already set.");

        return List.of(targetAttribute, ignoreEntries, overwriteAttribute, GroupNameFormatter.TO_LOWERCASE, GroupNameFormatter.TRIM_WHITESPACE, GroupNameFormatter.TRIM_PREFIX);
    }

    static class MapperConfig {
        Map<String, String> map;

        MapperConfig(Map<String, String> map) {
            this.map = map;
        }

        Stream<String> getIgnoreEntriesPatterns() {
            // Strings of type MULTIVALUED_STRING_TYPE are stored as a single string, delimited by "##".
            String ignoreEntriesRaw = map.getOrDefault(IGNORE_ENTRIES_PROPERTY, "");
            return Arrays.stream(ignoreEntriesRaw.split("##")).filter(s -> !"".equals(s));
        }

        String getTargetAttributeKey() {
            return map.getOrDefault(TARGET_ATTRIBUTE_PROPERTY, "");
        }

        String getTrimPrefix() {
            return map.getOrDefault(GroupNameFormatter.TRIM_PREFIX_PROPERTY, "");
        }

        boolean enabledTrimWhitespace() {
            return Boolean.parseBoolean(map.getOrDefault(GroupNameFormatter.TRIM_WHITESPACE_PROPERTY, String.valueOf(true)));
        }

        boolean enabledToLowerCase() {
            return Boolean.parseBoolean(map.getOrDefault(GroupNameFormatter.TO_LOWERCASE_PROPERTY, String.valueOf(true)));
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
