package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

class DefaultOrganizationMapperTest {

    @Test
    void testAssignDefaultOrganization_GivenExistingAttributeValue_ThenSkipOverwrite() {
        UserModel user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(DefaultOrganizationMapper.defaultTargetAttribute))
                .thenReturn(Stream.of("sapphire-stars"));

        DefaultOrganizationMapper subject = new DefaultOrganizationMapper();
        subject.assignDefaultOrganization(user, new DefaultOrganizationMapper.MapperConfig(Collections.emptyMap()));

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void testAssignDefaultOrganization_GivenNonExistingAttributeValue_ThenSetAttribute() {
        UserModel user = Mockito.mock(UserModel.class);
        GroupModel group = Mockito.mock(GroupModel.class);

        Mockito.when(user.getAttributeStream(Mockito.anyString()))
                .thenReturn(Stream.of(""));
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group));

        Mockito.when(group.getName()).thenReturn("sapphire-stars");
        DefaultOrganizationMapper subject = new DefaultOrganizationMapper();
        DefaultOrganizationMapper.MapperConfig config = new DefaultOrganizationMapper.MapperConfig(new HashMap<>());
        setIgnoreGroups(config);
        subject.assignDefaultOrganization(user, config);

        Mockito.verify(user).setAttribute(DefaultOrganizationMapper.defaultTargetAttribute, List.of("sapphire-stars"));
    }

    @Test
    void testAssignDefaultOrganization_GivenNonExistingAttributeValue_WhenGroupIgnore_ThenSkipAttribute() {
        UserModel user = Mockito.mock(UserModel.class);
        GroupModel group = Mockito.mock(GroupModel.class);

        Mockito.when(user.getAttributeStream(DefaultOrganizationMapper.defaultTargetAttribute))
                .thenReturn(Stream.of(""));
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group));

        Mockito.when(group.getName()).thenReturn("sapphire-stars");
        DefaultOrganizationMapper subject = new DefaultOrganizationMapper();
        DefaultOrganizationMapper.MapperConfig config = new DefaultOrganizationMapper.MapperConfig(new HashMap<>());
        setIgnoreGroups(config, "sapphire.*");
        subject.assignDefaultOrganization(user, config);

        Mockito.verify(user, Mockito.never()).setAttribute(DefaultOrganizationMapper.defaultTargetAttribute, List.of("sapphire-stars"));
    }

    @Test
    void testAssignDefaultOrganization_GivenMultipleGroups_ThenSkipUpdate() {
        UserModel user = Mockito.mock(UserModel.class);
        GroupModel group1 = Mockito.mock(GroupModel.class);
        GroupModel group2 = Mockito.mock(GroupModel.class);

        Mockito.when(user.getAttributeStream(DefaultOrganizationMapper.defaultTargetAttribute))
                .thenReturn(Stream.empty());
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group1, group2));

        Mockito.when(group1.getName()).thenReturn("sapphire-stars");
        Mockito.when(group2.getName()).thenReturn("rose-canyon");

        DefaultOrganizationMapper subject = new DefaultOrganizationMapper();
        DefaultOrganizationMapper.MapperConfig config = new DefaultOrganizationMapper.MapperConfig(new HashMap<>());
        setIgnoreGroups(config);
        subject.assignDefaultOrganization(user, config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    private void setIgnoreGroups(DefaultOrganizationMapper.MapperConfig mapperConfig, String... ignoreGroups) {
        mapperConfig.map.put(DefaultOrganizationMapper.IGNORE_GROUPS_PROPERTY, String.join("##", ignoreGroups));
    }
}
