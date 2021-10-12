package cloud.appuio.keycloak.extensions.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupToAttributeMapper extends AbstractIdentityProviderMapper {
    private static final Logger logger = Logger.getLogger(GroupToAttributeMapper.class);

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        this.assignGroupToAttribute(user, new MapperConfig(mapperModel.getConfig()));
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        this.assignGroupToAttribute(user, new MapperConfig(mapperModel.getConfig()));
    }

    void assignGroupToAttribute(UserModel user, MapperConfig config) {
        Set<String> defaultOrgAttribute = user.getAttributeStream(config.getTargetAttributeKey()).collect(Collectors.toSet());
        boolean isAlreadyDefined = defaultOrgAttribute.stream().anyMatch(value -> !"".equals(value));
        if (isAlreadyDefined) {
            logger.debugf("Attribute [%s] is already set for user [%s]: [%s]",
                    config.getTargetAttributeKey(),
                    user.getUsername(),
                    String.join(", ", defaultOrgAttribute)
            );
            return;
        }
        List<GroupModel> filteredGroups = user.getGroupsStream()
                .filter(this::ignoreEmptyGroupNames)
                .filter(groupModel -> ignoreGroupNamesThatMatchRegex(config, groupModel))
                .collect(Collectors.toList());

        if (filteredGroups.size() != 1) {
            logger.warnf("Cannot reduce group memberships to 1 group for [%s]. User is in following groups: [%s].",
                    user.getUsername(),
                    filteredGroups.stream().map(GroupModel::getName).collect(Collectors.joining(", ")));
            return;
        }
        GroupModel group = filteredGroups.get(0);
        user.setAttribute(config.getTargetAttributeKey(), List.of(group.getName()));
        logger.infof("Set the attribute [%s] for [%s] to [%s].", config.getTargetAttributeKey(), user.getUsername(), group.getName());
    }

    private boolean ignoreGroupNamesThatMatchRegex(MapperConfig config, GroupModel groupModel) {
        return config.getIgnoreGroups().noneMatch(toIgnore -> groupModel.getName().matches(toIgnore));
    }

    private boolean ignoreEmptyGroupNames(GroupModel group) {
        return !"".equals(group.getName());
    }

    // Boilerplate

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Group to Attribute";
    }

    @Override
    public String getHelpText() {
        return "Guesses the 'primary' group membership of a user and updates a user's attribute if not present. " +
                "It skips users where the group memberships cannot be reliably reduced to one entry. " +
                "The intention is to configure the ignore groups pattern so that all irrelevant group have been ruled out. " +
                "The mapper assumes that the group memberships are already up-to-date for the user and doesn't alter group memberships.";
    }

    public static final String IGNORE_GROUPS_PROPERTY = "ignore_groups";
    public static final String TARGET_ATTRIBUTE_PROPERTY = "target_attribute";

    static final String defaultTargetAttribute = "appuio.io/default-organization";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty ignoreGroups = new ProviderConfigProperty(
                IGNORE_GROUPS_PROPERTY, "Ignore groups", null, ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null
        );
        ignoreGroups.setHelpText("The user might be in groups that you want to ignore. " +
                "Each entry is a regex that is matched against each group name. " +
                "If any pattern matches, the group is ignored. " +
                "NOTE: Do NOT specify 2 or more '#' in sequence per pattern.");

        ProviderConfigProperty targetAttribute = new ProviderConfigProperty(
                TARGET_ATTRIBUTE_PROPERTY, "Target attribute", null, ProviderConfigProperty.STRING_TYPE, defaultTargetAttribute
        );
        targetAttribute.setHelpText("The user attribute key where the group name is stored in. " +
                "If this attribute is already set with a non-empty string, it will not be updated. " +
                "Defaults to '" + defaultTargetAttribute + "'.");

        return List.of(ignoreGroups, targetAttribute);
    }

    static class MapperConfig {
        Map<String, String> map;

        MapperConfig(Map<String, String> map) {
            this.map = map;
        }

        Stream<String> getIgnoreGroups() {
            // Strings of type MULTIVALUED_STRING_TYPE are stored as a single string, delimited by "##".
            String ignoreGroupRaw = map.getOrDefault(IGNORE_GROUPS_PROPERTY, "");
            String[] ignoreGroups = ignoreGroupRaw.split("##");
            return Arrays.stream(ignoreGroups).filter(s -> !"".equals(s));
        }

        String getTargetAttributeKey() {
            return map.getOrDefault(TARGET_ATTRIBUTE_PROPERTY, defaultTargetAttribute);
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
        return "group-to-attribute-mapper";
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return Arrays.asList(IdentityProviderSyncMode.IMPORT, IdentityProviderSyncMode.FORCE).contains(syncMode);
    }
}
