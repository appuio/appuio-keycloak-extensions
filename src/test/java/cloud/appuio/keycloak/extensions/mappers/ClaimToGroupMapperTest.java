package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.models.GroupModel;
import org.mockito.Mockito;

import java.util.HashSet;
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
    void testFindGroupsToBeAdded() {
        Set<GroupModel> currentGroups = new HashSet<>();

        var group1 = Mockito.mock(GroupModel.class);
        currentGroups.add(group1);
        var group2 = Mockito.mock(GroupModel.class);
        currentGroups.add(group2);

        var newGroup = Mockito.mock(GroupModel.class);
        Set<GroupModel> newGroups = new HashSet<>();
        newGroups.add(newGroup);

        var result = ClaimToGroupMapper.findGroupsToBeAdded(currentGroups, newGroups);

        assertThat(result).containsOnly(newGroup);
    }
}
