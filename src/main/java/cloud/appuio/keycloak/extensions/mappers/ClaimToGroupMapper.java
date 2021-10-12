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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class with the implementation of the identity provider mapper that sync the user's groups
 * received from an external IdP into the Keycloak groups.
 */
public class ClaimToGroupMapper extends AbstractClaimMapper {

    private static final Logger logger = Logger.getLogger(ClaimToGroupMapper.class);

    // global properties -------------------------------------

    private static final String PROVIDER_ID = "oidc-group-idp-mapper";

    private static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID
    };

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    private static final String CONTAINS_TEXT = "contains_text";

    private static final String CREATE_GROUPS = "create_groups";

    static {
        var claimProperty = new ProviderConfigProperty();
        claimProperty.setName(CLAIM);
        claimProperty.setLabel("Claim");
        claimProperty.setHelpText(
                "Name of claim to search for in token. This claim must be a string array with "
                        + "the names of the groups which the user is member. You can reference nested claims using a "
                        + "'.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        claimProperty.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(claimProperty);

        var containsTextProperty = new ProviderConfigProperty();
        containsTextProperty.setName(CONTAINS_TEXT);
        containsTextProperty.setLabel("Contains text");
        containsTextProperty.setHelpText(
                "Only sync groups that contains this text in its name. If empty, sync all groups.");
        containsTextProperty.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(containsTextProperty);

        var createGroupsProperty = new ProviderConfigProperty();
        createGroupsProperty.setName(CREATE_GROUPS);
        createGroupsProperty.setLabel("Create groups if not exists");
        createGroupsProperty.setHelpText(
                "Indicates if missing groups must be created in the realms. Otherwise, they will "
                        + "be ignored.");
        createGroupsProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        CONFIG_PROPERTIES.add(createGroupsProperty);
    }

    // properties --------------------------------------------

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
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
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    // actions -----------------------------------------------

    @Override
    public void importNewUser(
            KeycloakSession session,
            RealmModel realm,
            UserModel user,
            IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {
        super.importNewUser(session, realm, user, mapperModel, context);

        this.syncGroups(realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(
            KeycloakSession session,
            RealmModel realm,
            UserModel user,
            IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {

        this.syncGroups(realm, user, mapperModel, context);
    }

    private void syncGroups(
            RealmModel realm,
            UserModel user,
            IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {

        // check configurations
        var groupClaimName = mapperModel.getConfig().get(CLAIM);
        var containsText = mapperModel.getConfig().get(CONTAINS_TEXT);
        var createGroups = Boolean.valueOf(mapperModel.getConfig().get(CREATE_GROUPS));

        if (isEmpty(groupClaimName)) return;

        var claim = ClaimListExtractor.extractClaim(context, groupClaimName);
        if (claim.isEmpty()) {
            logger.debugf(
                    "Realm [%s], IdP [%s]: no group claim (claim name: [%s]) for user [%s], ignoring...",
                    realm.getName(),
                    mapperModel.getIdentityProviderAlias(),
                    groupClaimName,
                    user.getUsername());
            return;
        }

        logger.debugf(
                "Realm [%s], IdP [%s]: starting mapping groups for user [%s]",
                realm.getName(), mapperModel.getIdentityProviderAlias(), user.getUsername());

        // get user current groups
        var currentGroups = user.getGroupsStream()
                .filter(g -> isEmpty(containsText) || g.getName().contains(containsText))
                .collect(Collectors.toSet());

        logger.debugf(
                "Realm [%s], IdP [%s]: current groups for user [%s]: %s",
                realm.getName(),
                mapperModel.getIdentityProviderAlias(),
                user.getUsername(),
                currentGroups.stream().map(GroupModel::getName).collect(Collectors.joining(",")));

        // map and filter the groups by name
        var groupNamesFromClaim = claim.get().stream()
                .filter(t -> isEmpty(containsText) || t.contains(containsText))
                .map(this::replaceInvalidCharacters)
                .collect(Collectors.toSet());

        var newRealmGroups = filterNewRealmGroups(realm, groupNamesFromClaim, createGroups);

        logger.debugf(
                "Realm [%s], IdP [%s]: new groups for user [%s]: %s",
                realm.getName(),
                mapperModel.getIdentityProviderAlias(),
                user.getUsername(),
                newRealmGroups.stream().map(GroupModel::getName).collect(Collectors.joining(",")));

        // Leave the groups where the user is not member of
        findGroupsToBeRemoved(currentGroups, newRealmGroups).forEach(user::leaveGroup);

        // Join the groups where the user is not yet member of
        findGroupsToBeAdded(currentGroups, newRealmGroups).forEach(user::joinGroup);

        logger.debugf(
                "Realm [%s], IdP [%s]: finishing mapping groups for user [%s]",
                realm.getName(), mapperModel.getIdentityProviderAlias(), user.getUsername());
    }

    String replaceInvalidCharacters(String groupName) {
        return groupName.replaceFirst("^/", "").replace("/", "-");
    }

    /**
     * Returns a stream that contains the groups that do not yet exist in the realm.
     *
     * @return new set that contains only the groups to create
     */
    private Set<GroupModel> filterNewRealmGroups(
            RealmModel realm, Set<String> newGroupsNames, Boolean createGroups) {

        Set<GroupModel> groups = new HashSet<>();

        newGroupsNames.forEach(
                groupName -> {
                    Optional<GroupModel> group = findGroupByName(realm, groupName);

                    // create group if not found
                    group.ifPresentOrElse(
                            groups::add,
                            () -> {
                                if (createGroups) {
                                    createAndJoinGroup(realm, groups, groupName);
                                }
                            });
                });

        return groups;
    }

    private void createAndJoinGroup(RealmModel realm, Set<GroupModel> groups, String groupName) {
        logger.debugf("Realm [%s]: creating group [%s]", realm.getName(), groupName);

        var newGroup = realm.createGroup(groupName);
        groups.add(newGroup);
    }

    private static Optional<GroupModel> findGroupByName(RealmModel realm, String name) {
        return realm.getGroupsStream().filter(g -> g.getName().equals(name)).findFirst();
    }

    /**
     * Subtracts newGroups from currentGroups.
     */
    static Stream<GroupModel> findGroupsToBeRemoved(
            Set<GroupModel> currentGroups, Set<GroupModel> newGroups) {
        var resultSet = new HashSet<>(currentGroups);
        resultSet.removeAll(newGroups);
        return resultSet.stream();
    }

    /**
     * Subtracts current groups from newGroups.
     */
    static Stream<GroupModel> findGroupsToBeAdded(
            Set<GroupModel> currentGroups, Set<GroupModel> newGroups) {
        var resultSet = new HashSet<>(newGroups);

        // (New - Current) will result in a set with the groups where the user will be added
        resultSet.removeAll(currentGroups);
        return resultSet.stream();
    }

    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return Arrays.asList(IdentityProviderSyncMode.IMPORT, IdentityProviderSyncMode.FORCE).contains(syncMode);
    }
}
