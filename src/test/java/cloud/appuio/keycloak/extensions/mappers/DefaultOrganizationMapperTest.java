package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.stream.Stream;

class DefaultOrganizationMapperTest {

    DefaultOrganizationMapper subject;

    @Test
    void testAssignDefaultOrganization_GivenExistingAttributeValue_ThenSkipOverwrite() {
        UserModel user = Mockito.mock(UserModel.class);
        Mockito.when(user.getAttributeStream(DefaultOrganizationMapper.DEFAULT_ORGANIZATION_ATTRIBUTE_KEY))
                .thenReturn(Stream.of("sapphire-stars"));

        subject = new DefaultOrganizationMapper();

        subject.assignDefaultOrganization(user, null);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void testAssignDefaultOrganization_GivenNonExistingAttributeValue_ThenSetAttribute() {
        UserModel user = Mockito.mock(UserModel.class);
        GroupModel group = Mockito.mock(GroupModel.class);
        Mockito.when(user.getAttributeStream(DefaultOrganizationMapper.DEFAULT_ORGANIZATION_ATTRIBUTE_KEY))
                .thenReturn(Stream.of(""));
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group));
        Mockito.when(group.getName()).thenReturn("sapphire-stars");

        subject = new DefaultOrganizationMapper();

        subject.assignDefaultOrganization(user, null);

        Mockito.verify(user).setAttribute(DefaultOrganizationMapper.DEFAULT_ORGANIZATION_ATTRIBUTE_KEY, Collections.singletonList("sapphire-stars"));
    }

    @Test
    void testAssignDefaultOrganization_GivenMultipleGroups_ThenSkipUpdate() {
        UserModel user = Mockito.mock(UserModel.class);
        GroupModel group1 = Mockito.mock(GroupModel.class);
        GroupModel group2 = Mockito.mock(GroupModel.class);
        Mockito.when(user.getAttributeStream(DefaultOrganizationMapper.DEFAULT_ORGANIZATION_ATTRIBUTE_KEY))
                .thenReturn(Stream.empty());
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group1, group2));
        Mockito.when(group1.getName()).thenReturn("sapphire-stars");
        Mockito.when(group2.getName()).thenReturn("rose-canyon");

        subject = new DefaultOrganizationMapper();

        subject.assignDefaultOrganization(user, null);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }
}
