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
        String source = "/LDAP/some group";
        String target = "LDAP-some group";

        String result = new ClaimToGroupMapper().replaceInvalidCharacters(source);

        assertThat(result).isEqualTo(target);
    }

    @Test
    void testFindGroupsToBeAdded() {
        Set<GroupModel> currentGroups = new HashSet<>();

        GroupModel group1 = Mockito.mock(GroupModel.class);
        currentGroups.add(group1);
        GroupModel group2 = Mockito.mock(GroupModel.class);
        currentGroups.add(group2);

        GroupModel newGroup = Mockito.mock(GroupModel.class);
        Set<GroupModel> newGroups = new HashSet<>();
        newGroups.add(newGroup);

        Stream<GroupModel> result = ClaimToGroupMapper.findGroupsToBeAdded(currentGroups, newGroups);

        assertThat(result).containsOnly(newGroup);
    }
}
