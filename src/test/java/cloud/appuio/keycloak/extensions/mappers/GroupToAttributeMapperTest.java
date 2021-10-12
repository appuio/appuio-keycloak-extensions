package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

class GroupToAttributeMapperTest {

    @Test
    void testAssignGroupToAttribute_GivenExistingAttributeValue_ThenSkipOverwrite() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(GroupToAttributeMapper.defaultTargetAttribute))
                .thenReturn(Stream.of("sapphire-stars"));

        var subject = new GroupToAttributeMapper();
        subject.assignGroupToAttribute(user, new GroupToAttributeMapper.MapperConfig(Collections.emptyMap()));

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void testAssignGroupToAttribute_GivenNonExistingAttributeValue_ThenSetAttribute() {
        var user = Mockito.mock(UserModel.class);
        var group = Mockito.mock(GroupModel.class);

        Mockito.when(user.getAttributeStream(Mockito.anyString()))
                .thenReturn(Stream.of(""));
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group));

        Mockito.when(group.getName()).thenReturn("sapphire-stars");
        var subject = new GroupToAttributeMapper();
        var config = new GroupToAttributeMapper.MapperConfig(new HashMap<>());
        setIgnoreGroups(config);
        subject.assignGroupToAttribute(user, config);

        Mockito.verify(user).setAttribute(GroupToAttributeMapper.defaultTargetAttribute, List.of("sapphire-stars"));
    }

    @Test
    void testAssignGroupToAttribute_GivenNonExistingAttributeValue_WhenGroupIgnore_ThenSkipAttribute() {
        var user = Mockito.mock(UserModel.class);
        var group = Mockito.mock(GroupModel.class);

        Mockito.when(user.getAttributeStream(GroupToAttributeMapper.defaultTargetAttribute))
                .thenReturn(Stream.of(""));
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group));

        Mockito.when(group.getName()).thenReturn(" Sapphire Stars");
        var subject = new GroupToAttributeMapper();
        var config = new GroupToAttributeMapper.MapperConfig(new HashMap<>());
        config.map.put(GroupNameFormatter.TO_LOWERCASE_PROPERTY, "true");
        config.map.put(GroupNameFormatter.TRIM_WHITESPACE_PROPERTY, "true");
        setIgnoreGroups(config, "\\s*Sapphire.*");
        subject.assignGroupToAttribute(user, config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void testAssignGroupToAttribute_GivenMultipleGroups_ThenSkipUpdate() {
        var user = Mockito.mock(UserModel.class);
        var group1 = Mockito.mock(GroupModel.class);
        var group2 = Mockito.mock(GroupModel.class);

        Mockito.when(user.getAttributeStream(GroupToAttributeMapper.defaultTargetAttribute))
                .thenReturn(Stream.empty());
        Mockito.when(user.getGroupsStream())
                .thenReturn(Stream.of(group1, group2));

        Mockito.when(group1.getName()).thenReturn("sapphire-stars");
        Mockito.when(group2.getName()).thenReturn("rose-canyon");

        var subject = new GroupToAttributeMapper();
        var config = new GroupToAttributeMapper.MapperConfig(new HashMap<>());
        setIgnoreGroups(config);
        subject.assignGroupToAttribute(user, config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    private void setIgnoreGroups(GroupToAttributeMapper.MapperConfig mapperConfig, String... ignoreGroups) {
        mapperConfig.map.put(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, String.join("##", ignoreGroups));
    }
}
