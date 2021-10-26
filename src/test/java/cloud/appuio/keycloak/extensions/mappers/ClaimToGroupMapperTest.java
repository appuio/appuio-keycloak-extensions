package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimToGroupMapperTest {

    @Test
    void testSyncGroups_GivenListWithNewGroup_WhenCreateEnabled_ThenCreateAndJoinGroup() {
        var realm = Mockito.mock(RealmModel.class);
        var user = Mockito.mock(UserModel.class);
        var createdGroup = Mockito.mock(GroupModel.class);

        //noinspection unchecked
        Mockito.when(realm.getGroupsStream()).thenReturn(Stream.empty(), Stream.of(createdGroup));
        Mockito.when(createdGroup.getName()).thenReturn("Rose Canyon");

        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();
        setCreateGroupEnabled(config);

        subject.doSyncGroups(realm, user, List.of("Rose Canyon"), newInstrumentation(), config);

        Mockito.verify(realm).createGroup("Rose Canyon");
        Mockito.verify(user).joinGroup(createdGroup);
        Mockito.verify(user, Mockito.never()).leaveGroup(Mockito.any());
    }

    @Test
    void testSyncGroups_GivenListWithNewGroup_WhenCreateDisabled_ThenDoNothing() {
        var realm = Mockito.mock(RealmModel.class);
        var user = Mockito.mock(UserModel.class);

        Mockito.when(realm.getGroupsStream()).thenReturn(Stream.empty());

        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();

        subject.doSyncGroups(realm, user, List.of("Rose Canyon"), newInstrumentation(), config);

        Mockito.verify(realm, Mockito.never()).createGroup(Mockito.anyString());
        Mockito.verify(user, Mockito.never()).joinGroup(Mockito.any());
        Mockito.verify(user, Mockito.never()).leaveGroup(Mockito.any());
    }

    @Test
    void testSyncGroups_GivenListWithNewGroup_WhenGroupExists_ThenJoinGroup() {
        var realm = Mockito.mock(RealmModel.class);
        var user = Mockito.mock(UserModel.class);
        var group = Mockito.mock(GroupModel.class);

        Mockito.when(realm.getGroupsStream()).thenReturn(Stream.of(group));
        Mockito.when(group.getName()).thenReturn("Rose Canyon");

        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();

        subject.doSyncGroups(realm, user, List.of("Rose Canyon"), newInstrumentation(), config);

        Mockito.verify(realm, Mockito.never()).createGroup(Mockito.anyString());
        Mockito.verify(user).joinGroup(group);
        Mockito.verify(user, Mockito.never()).leaveGroup(Mockito.any());
    }

    @Test
    void testSyncGroups_GivenListWithAbsentGroup_WhenUserInExistingGroup_ThenLeaveGroup() {
        var realm = Mockito.mock(RealmModel.class);
        var user = Mockito.mock(UserModel.class);
        var groupToRemove = Mockito.mock(GroupModel.class);
        var groupToKeep = Mockito.mock(GroupModel.class);

        Mockito.when(user.getGroupsStream()).thenReturn(Stream.of(groupToRemove, groupToKeep));
        Mockito.when(groupToKeep.getName()).thenReturn("keep group");

        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();

        subject.doSyncGroups(realm, user, List.of("keep group"), newInstrumentation(), config);

        Mockito.verify(realm, Mockito.never()).createGroup(Mockito.anyString());
        Mockito.verify(user, Mockito.never()).joinGroup(Mockito.any());
        Mockito.verify(user).leaveGroup(groupToRemove);
        Mockito.verify(user, Mockito.never()).leaveGroup(groupToKeep);
    }

    @Test
    void testFilterGroupNames_GivenEmptyListOfPattern_WhenDefaultConfig_ThenReturnUnformatted() {
        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();

        var result = subject.filterGroupNames(List.of("Rose Canyon"), config);

        assertThat(result).containsExactly("Rose Canyon");
    }

    @Test
    void testFilterGroupNames_GivenListOfPattern_WhenPatternMatches_ThenReturnFormatted() {
        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();
        setIncludePattern(config, "^Rose.*");
        setLowerCase(config);
        setWhiteSpace(config);

        var result = subject.filterGroupNames(List.of("Rose Canyon", "Sapphire Stars"), config);

        assertThat(result).containsExactly("rose-canyon");
    }

    @Test
    void testFilterGroupNames_GivenListOfPattern_WhenPatternDoesNotMatch_ThenReturnEmpty() {
        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();
        setIncludePattern(config, "^rose.*");

        var result = subject.filterGroupNames(List.of("Rose Canyon"), config);

        assertThat(result).isEmpty();
    }

    private ClaimToGroupMapper.Instrumentation newInstrumentation() {
        return new ClaimToGroupMapper.Instrumentation("realm", "idp", "user");
    }

    private void setCreateGroupEnabled(ClaimToGroupMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(ClaimToGroupMapper.CREATE_GROUPS, Boolean.toString(true));
    }

    private void setIncludePattern(ClaimToGroupMapper.MapperConfig mapperConfig, String pattern) {
        mapperConfig.map.put(ClaimToGroupMapper.INCLUDE_PATTERNS, pattern);
    }

    private void setLowerCase(ClaimToGroupMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(GroupNameFormatter.TO_LOWERCASE_PROPERTY, Boolean.toString(true));
    }

    private void setWhiteSpace(ClaimToGroupMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(GroupNameFormatter.TRIM_WHITESPACE_PROPERTY, Boolean.toString(true));
    }

    private ClaimToGroupMapper.MapperConfig newMapperConfig() {
        var config = new ClaimToGroupMapper.MapperConfig(new HashMap<>());
        config.map.put(AbstractClaimMapper.CLAIM, "groups");
        return config;
    }
}
