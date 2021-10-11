package cloud.appuio.keycloak.extensions.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class DefaultOrganizationMapper extends AbstractClaimMapper {
    private static final Logger logger = Logger.getLogger(DefaultOrganizationMapper.class);

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);
        this.assignDefaultOrganization(realm, user, mapperModel);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
        this.assignDefaultOrganization(realm, user, mapperModel);
    }

    void assignDefaultOrganization(RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel) {

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
