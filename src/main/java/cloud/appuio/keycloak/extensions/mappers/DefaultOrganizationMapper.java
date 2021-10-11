package cloud.appuio.keycloak.extensions.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultOrganizationMapper extends AbstractClaimMapper {
    private static final Logger logger = Logger.getLogger(DefaultOrganizationMapper.class);

    public static final String DEFAULT_ORGANIZATION_ATTRIBUTE_KEY = "appuio.io/default-organization";

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);
        this.assignDefaultOrganization(user, mapperModel);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
        this.assignDefaultOrganization(user, mapperModel);
    }

    void assignDefaultOrganization(UserModel user, IdentityProviderMapperModel mapperModel) {
        Set<String> defaultOrgAttribute = user.getAttributeStream(DEFAULT_ORGANIZATION_ATTRIBUTE_KEY).collect(Collectors.toSet());
        boolean isAlreadyDefined = defaultOrgAttribute.stream().anyMatch(value -> !"".equals(value));
        if (isAlreadyDefined) {
            logger.debugf("Default organization is already set for user [%s]: [%s]",
                    user.getUsername(),
                    String.join(", ", defaultOrgAttribute)
            );
            return;
        }
        Set<GroupModel> groups = user.getGroupsStream()
                .filter(group -> !"".equals(group.getName()))
                .filter(groupModel -> true) // TODO: Ignore certain groups
                .collect(Collectors.toSet());

        if (groups.size() != 1) {
            logger.warnf("Cannot determine default organization for [%s]. User is in multiple groups: [%s]. This may require manual action.",
                    user.getUsername(),
                    groups.stream().map(GroupModel::getName).collect(Collectors.joining(", ")));
            return;
        }
        groups.stream().findFirst().ifPresent(group -> {
                    user.setAttribute(DEFAULT_ORGANIZATION_ATTRIBUTE_KEY, Collections.singletonList(group.getName()));
                    logger.infof("Set the default organization for [%s] to [%s].", user.getUsername(), group.getName());
                }
        );
    }

    // Boilerplate

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Default organization to attribute mapper";
    }

    @Override
    public String getHelpText() {
        return "Guesses the 'primary' group membership of a user and updates the user's default-organization attribute if not present. " +
                "It skips users where the default organization cannot be reliably determined. " +
                "The mapper assumes that the group memberships are already up-to-date for the user (use other mappers first to set the group memberships).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>();
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
}
