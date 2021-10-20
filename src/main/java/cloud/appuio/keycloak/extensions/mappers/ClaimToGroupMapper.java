/*
Original author: Luiz Carlos Viana Melo
Original project: https://github.com/amomra/oidc-group-mapper
Original license: MIT

This file was originally written by mentioned author, but here it was brought up-to-date and modified for
APPUiO Cloud purposes. Many thanks to Luiz Carlos Viana Melo for the inspiring works!
 */

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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class with the implementation of the identity provider mapper that sync the user's groups
 * received from an external IdP into the Keycloak groups.
 */
public class ClaimToGroupMapper extends AbstractClaimMapper {

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        this.syncGroups(realm, user, new MapperConfig(mapperModel.getConfig()), mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        this.syncGroups(realm, user, new MapperConfig(mapperModel.getConfig()), mapperModel, context);
    }

    private void syncGroups(RealmModel realm, UserModel user, MapperConfig config, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // abort if mapper is configured incorrectly.
        if (config.getClaimName().equals("")) return;

        var instrumentation = new Instrumentation(realm.getName(), mapperModel.getIdentityProviderAlias(), user.getUsername());

        var claim = ClaimListExtractor.extractClaim(context, config.getClaimName());
        if (claim.isEmpty()) {
            instrumentation.noClaimForUser(config.getClaimName());
            return;
        }
        doSyncGroups(realm, user, claim.get(), instrumentation, config);
    }

    void doSyncGroups(RealmModel realm, UserModel user, List<String> rawGroupNames, Instrumentation instrumentation, MapperConfig config) {
        var filteredGroupNames = filterGroupNames(rawGroupNames, config);

        if (config.enabledCreateGroups()) {
            createMissingGroups(realm, filteredGroupNames, instrumentation);
        }

        leaveGroupsNotInClaim(user, filteredGroupNames, instrumentation);
        joinGroupsInClaim(realm, user, filteredGroupNames, instrumentation);
    }

    Set<String> filterGroupNames(List<String> rawGroupNames, MapperConfig config) {
        var formatter = new GroupNameFormatter()
                .withTrimWhitespace(config.enabledTrimWhitespace())
                .withTrimPrefix(config.getTrimPrefix())
                .withToLowerCase(config.enabledToLowerCase());

        return rawGroupNames.stream()
                .filter(rawName -> matchesAnyPattern(config.getIncludePatterns(), rawName))
                .map(formatter::format)
                .collect(Collectors.toSet());
    }

    private boolean matchesAnyPattern(List<String> patterns, String rawName) {
        return patterns.isEmpty() || patterns.stream().anyMatch(rawName::matches);
    }

    private void joinGroupsInClaim(RealmModel realm, UserModel user, Set<String> groupNamesInClaim, Instrumentation instrumentation) {
        var joinedGroupNames = realm.getGroupsStream()
                .filter(group -> groupNamesInClaim.contains(group.getName()))
                .filter(group -> !user.isMemberOf(group))
                .peek(user::joinGroup)
                .map(GroupModel::getName)
                .collect(Collectors.joining(", "));
        instrumentation.joinedGroups(joinedGroupNames);
    }

    private void leaveGroupsNotInClaim(UserModel user, Set<String> groupNamesInClaim, Instrumentation instrumentation) {
        var leftGroupNames = user.getGroupsStream()
                .filter(group -> !groupNamesInClaim.contains(group.getName()))
                .peek(user::leaveGroup)
                .map(GroupModel::getName)
                .collect(Collectors.joining(", "));
        instrumentation.leftGroups(leftGroupNames);
    }

    private void createMissingGroups(RealmModel realm, Set<String> groupNames, Instrumentation instrumentation) {
        var newGroupNames = groupNames.stream()
                .filter(groupName -> realm.getGroupsStream()
                        .map(GroupModel::getName)
                        .noneMatch(existingGroup -> existingGroup.equals(groupName)))
                .peek(realm::createGroup)
                .collect(Collectors.joining(", "));
        instrumentation.createdGroups(newGroupNames);
    }

    public static final String INCLUDE_PATTERNS = "include_patterns";
    public static final String CREATE_GROUPS = "create_groups";

    static class MapperConfig {
        Map<String, String> map;

        MapperConfig(Map<String, String> config) {
            this.map = config;
        }

        String getClaimName() {
            return map.getOrDefault(CLAIM, "");
        }

        boolean enabledTrimWhitespace() {
            return Boolean.parseBoolean(map.getOrDefault(GroupNameFormatter.TRIM_WHITESPACE_PROPERTY, String.valueOf(false)));
        }

        boolean enabledToLowerCase() {
            return Boolean.parseBoolean(map.getOrDefault(GroupNameFormatter.TO_LOWERCASE_PROPERTY, String.valueOf(false)));
        }

        String getTrimPrefix() {
            return map.getOrDefault(GroupNameFormatter.TRIM_PREFIX_PROPERTY, "");
        }

        List<String> getIncludePatterns() {
            // Strings of type MULTIVALUED_STRING_TYPE are stored as a single string, delimited by "##".
            String rawPatterns = map.getOrDefault(INCLUDE_PATTERNS, "");
            return Arrays.stream(rawPatterns.split("##")).filter(s -> !"".equals(s)).collect(Collectors.toList());
        }

        boolean enabledCreateGroups() {
            return Boolean.parseBoolean(map.getOrDefault(CREATE_GROUPS, String.valueOf(false)));
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        var claimProperty = new ProviderConfigProperty(
                CLAIM, "Claim name", null, ProviderConfigProperty.STRING_TYPE, ""
        );
        claimProperty.setHelpText("**REQUIRED** Name of claim to search for in token. " +
                "This claim must be a string array with the names of the groups which the user is member. " +
                "You can reference nested claims using a '.', i.e. 'address.locality'. " +
                "To use dot (.) literally, escape it with backslash (\\.)");

        var includePatternsProperty = new ProviderConfigProperty(
                INCLUDE_PATTERNS, "Match patterns", null, ProviderConfigProperty.MULTIVALUED_STRING_TYPE, ""
        );
        includePatternsProperty.setHelpText("Only sync groups when their name matches one of the given pattern. " +
                "If empty, all groups are synced. " +
                "The patterns are matched before trimming whitespaces or prefix and before lowering case.");

        var createGroupsProperty = new ProviderConfigProperty(
                CREATE_GROUPS, "Create groups if not exists", null, ProviderConfigProperty.BOOLEAN_TYPE, false
        );
        createGroupsProperty.setHelpText("Indicates if missing groups must be created in the realms. " +
                "Otherwise, they will be ignored.");

        return List.of(claimProperty, includePatternsProperty, createGroupsProperty, GroupNameFormatter.TO_LOWERCASE, GroupNameFormatter.TRIM_WHITESPACE, GroupNameFormatter.TRIM_PREFIX);
    }

    @Override
    public String getId() {
        return "oidc-group-idp-mapper";
    }

    @Override
    public String[] getCompatibleProviders() {
        return new String[]{
                KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID
        };
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Claim to Group";
    }

    @Override
    public String getHelpText() {
        return "If a claim exists, sync the IdP user's groups with realm groups";
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return Arrays.asList(IdentityProviderSyncMode.IMPORT, IdentityProviderSyncMode.FORCE).contains(syncMode);
    }

    /**
     * This class is meant to remove boilerplate from business logic by moving the logging calls but still provide meaning.
     */
    static class Instrumentation {
        private static final Logger logger = Logger.getLogger(ClaimToGroupMapper.class);
        private final String idpAlias;
        private final String username;
        private final String realmName;

        Instrumentation(String realmName, String identityProviderAlias, String username) {
            this.realmName = realmName;
            this.idpAlias = identityProviderAlias;
            this.username = username;
        }

        void noClaimForUser(String claimName) {
            logger.debugf("Realm [%s], IdP [%s]: user [%s] has no claim: [%s], ignoring...",
                    this.realmName, this.idpAlias, this.username, claimName);
        }

        void createdGroups(String newGroupNames) {
            logger.debugf("Realm [%s], IdP [%s]: created new groups for user [%s]: [%s]",
                    this.realmName, this.idpAlias, this.username, newGroupNames);
        }

        void joinedGroups(String joinedGroups) {
            logger.debugf("Realm [%s], IdP [%s]: user [%s] joined groups: [%s]",
                    this.realmName, this.idpAlias, this.username, joinedGroups);
        }

        void leftGroups(String leftGroupNames) {
            logger.debugf("Realm [%s], IdP [%s]: user [%s] left groups: [%s]",
                    this.realmName, this.idpAlias, this.username, leftGroupNames);
        }
    }
}
