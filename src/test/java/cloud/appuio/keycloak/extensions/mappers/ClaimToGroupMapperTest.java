package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimToGroupMapperTest {

    @Test
    void testReplaceInvalidCharacters() {
        var source = "/LDAP/some group";
        var target = "LDAP-some group";

        var result = new ClaimToGroupMapper().replaceInvalidCharacters(source);

        assertThat(result).isEqualTo(target);
    }

    @Test
    void testSyncGroups_GivenListWithNewGroup_WhenCreateEnabled_ThenCreateAndJoinGroup() {
        var realm = Mockito.mock(RealmModel.class);
        var user = Mockito.mock(UserModel.class);

        Mockito.when(realm.getGroupsStream()).thenReturn(Stream.empty(), Stream.empty());

        var subject = new ClaimToGroupMapper();
        var config = newMapperConfig();
        setCreateGroupEnabled(config);

        subject.doSyncGroups(realm, user, List.of("newGroup"), newInstrumentation(), config);

        Mockito.verify(realm).createGroup("newgroup");
    }

    private ClaimToGroupMapper.Instrumentation newInstrumentation() {
        return new ClaimToGroupMapper.Instrumentation("realm", "idp", "user");
    }

    private void setCreateGroupEnabled(ClaimToGroupMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(ClaimToGroupMapper.CREATE_GROUPS, Boolean.toString(true));
    }

    private ClaimToGroupMapper.MapperConfig newMapperConfig() {
        var config = new ClaimToGroupMapper.MapperConfig(new HashMap<>());
        config.map.put(AbstractClaimMapper.CLAIM, "groups");
        return config;
    }
}
