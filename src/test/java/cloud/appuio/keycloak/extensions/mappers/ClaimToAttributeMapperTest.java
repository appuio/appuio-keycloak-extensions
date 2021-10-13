package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

class ClaimToAttributeMapperTest {

    String realmName = "realm";
    String idpAlias = "idp";
    String attributeKey = "attribute";

    @Test
    void testAssignGroupToAttribute_GivenExistingAttributeValue_WhenOverwriteDisabled_ThenSkipOverwrite() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(attributeKey))
                .thenReturn(Stream.of("sapphire-stars"));

        var subject = new ClaimToAttributeMapper();
        var config = newMapperConfig();
        setIgnoreGroups(config);
        subject.assignClaimToAttribute(realmName, idpAlias, user, Collections.emptyList(), config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void testAssignGroupToAttribute_GivenExistingEmptyAttribute_WhenOverwriteDisabled_ThenSet() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(attributeKey))
                .thenReturn(Stream.of(""));

        var subject = new ClaimToAttributeMapper();
        var config = newMapperConfig();
        setIgnoreGroups(config);
        subject.assignClaimToAttribute(realmName, idpAlias, user, List.of("sapphire-stars"), config);

        Mockito.verify(user).setAttribute(attributeKey, List.of("sapphire-stars"));
    }

    @Test
    void testAssignGroupToAttribute_GivenExistingAttributeValue_WhenOverwriteEnabled_ThenOverwrite() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(attributeKey))
                .thenReturn(Stream.of("sapphire-stars"));

        var subject = new ClaimToAttributeMapper();
        var config = newMapperConfig();
        setIgnoreGroups(config);
        setOverwriteEnabled(config);
        subject.assignClaimToAttribute(realmName, idpAlias, user, List.of("rose-canyon"), config);

        Mockito.verify(user).setAttribute(attributeKey, List.of("rose-canyon"));
    }

    @Test
    void testAssignGroupToAttribute_GivenNonExistingAttributeValue_ThenSetAttribute() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(Mockito.anyString()))
                .thenReturn(Stream.of(""));
        var subject = new ClaimToAttributeMapper();
        var config = newMapperConfig();
        setIgnoreGroups(config);
        subject.assignClaimToAttribute(realmName, idpAlias, user, List.of("sapphire-stars"), config);

        Mockito.verify(user).setAttribute(attributeKey, List.of("sapphire-stars"));
    }

    @Test
    void testAssignGroupToAttribute_GivenNonExistingAttributeValue_WhenGroupIgnore_ThenSkipAttribute() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(attributeKey))
                .thenReturn(Stream.of(""));
        var subject = new ClaimToAttributeMapper();
        var config = newMapperConfig();
        setWhiteSpace(config);
        setLowerCase(config);
        setIgnoreGroups(config, "\\s*Sapphire.*");

        subject.assignClaimToAttribute(realmName, idpAlias, user, List.of(" Sapphire Stars"), config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void testAssignGroupToAttribute_GivenMultipleGroups_ThenSkipUpdate() {
        var user = Mockito.mock(UserModel.class);

        Mockito.when(user.getAttributeStream(attributeKey))
                .thenReturn(Stream.empty());

        var subject = new ClaimToAttributeMapper();
        var config = newMapperConfig();
        setIgnoreGroups(config);

        subject.assignClaimToAttribute(realmName, idpAlias, user, List.of("sapphire-stars", "rose-canyon"), config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    private void setIgnoreGroups(ClaimToAttributeMapper.MapperConfig mapperConfig, String... ignoreGroups) {
        mapperConfig.map.put(ClaimToAttributeMapper.IGNORE_ENTRIES_PROPERTY, String.join("##", ignoreGroups));
    }

    private void setLowerCase(ClaimToAttributeMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(GroupNameFormatter.TO_LOWERCASE_PROPERTY, Boolean.toString(true));
    }

    private void setWhiteSpace(ClaimToAttributeMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(GroupNameFormatter.TRIM_WHITESPACE_PROPERTY, Boolean.toString(true));
    }

    private void setOverwriteEnabled(ClaimToAttributeMapper.MapperConfig mapperConfig) {
        mapperConfig.map.put(ClaimToAttributeMapper.OVERWRITE_ATTRIBUTE_PROPERTY, Boolean.toString(true));
    }

    private ClaimToAttributeMapper.MapperConfig newMapperConfig() {
        var config = new ClaimToAttributeMapper.MapperConfig(new HashMap<>());
        config.map.put(AbstractClaimMapper.CLAIM, "groups");
        config.map.put(ClaimToAttributeMapper.TARGET_ATTRIBUTE_PROPERTY, attributeKey);
        return config;
    }
}
