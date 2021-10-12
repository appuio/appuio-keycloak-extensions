package cloud.appuio.keycloak.extensions.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultOrganizationMapper extends AbstractClaimMapper {
    private static final Logger logger = Logger.getLogger(DefaultOrganizationMapper.class);

    public static final String DEFAULT_ORGANIZATION_ATTRIBUTE_KEY = "appuio.io/default-organization";

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);
        this.assignDefaultOrganization(user, new MapperConfig(mapperModel.getConfig()));
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
        this.assignDefaultOrganization(user, new MapperConfig(mapperModel.getConfig()));
    }

    void assignDefaultOrganization(UserModel user, MapperConfig config) {
        Set<String> defaultOrgAttribute = user.getAttributeStream(DEFAULT_ORGANIZATION_ATTRIBUTE_KEY).collect(Collectors.toSet());
        boolean isAlreadyDefined = defaultOrgAttribute.stream().anyMatch(value -> !"".equals(value));
        if (isAlreadyDefined) {
            logger.debugf("Default organization is already set for user [%s]: [%s]",
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
            logger.warnf("Cannot determine default organization for [%s]. User is in following groups: [%s]. This may require manual action.",
                    user.getUsername(),
                    filteredGroups.stream().map(GroupModel::getName).collect(Collectors.joining(", ")));
            return;
        }
        GroupModel group = filteredGroups.get(0);
        user.setAttribute(DEFAULT_ORGANIZATION_ATTRIBUTE_KEY, List.of(group.getName()));
        logger.infof("Set the default organization for [%s] to [%s].", user.getUsername(), group.getName());
    }

    private boolean ignoreGroupNamesThatMatchRegex(MapperConfig config, GroupModel groupModel) {
        return config.getIgnoreGroups().noneMatch(toIgnore -> groupModel.getName().matches(toIgnore));
    }

    private boolean ignoreEmptyGroupNames(GroupModel group) {
        return !"".equals(group.getName());
    }

    // Boilerplate

    static class MapperConfig {
        Map<String, String> map;

        MapperConfig(Map<String, String> map) {
            this.map = map;
        }

        Stream<String> getIgnoreGroups() {
            // Strings of type MULTIVALUED_STRING_TYPE are stored as a single string, delimited by "##".
            String ignoreGroupRaw = map.get(IGNORE_GROUPS_PROPERTY);
            String[] ignoreGroups = ignoreGroupRaw.split("##");
            return Arrays.stream(ignoreGroups).filter(s -> !"".equals(s));
        }
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Default Organization to Attribute";
    }

    @Override
    public String getHelpText() {
        return "Guesses the 'primary' group membership of a user and updates the user's default-organization attribute if not present. " +
                "It skips users where the default organization cannot be reliably determined. " +
                "The mapper assumes that the group memberships are already up-to-date for the user (use other mappers first to set the group memberships).";
    }

    public static String IGNORE_GROUPS_PROPERTY = "ignore_groups";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> props = new ArrayList<>();

        ProviderConfigProperty ignoreGroups = new ProviderConfigProperty(
                IGNORE_GROUPS_PROPERTY, "Ignore groups", null, ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null
        );
        ignoreGroups.setHelpText("The user might be in groups that you want to ignore. " +
                "Each entry is a regex that is matched against each group name. " +
                "If any pattern matches, the group is ignored. "+
                "NOTE: Do NOT specify 2 or more '#' in sequence per pattern.");

        props.add(ignoreGroups);
        return props;
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
        return "appuio-default-organization-mapper";
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return Arrays.asList(IdentityProviderSyncMode.IMPORT, IdentityProviderSyncMode.FORCE).contains(syncMode);
    }
}
