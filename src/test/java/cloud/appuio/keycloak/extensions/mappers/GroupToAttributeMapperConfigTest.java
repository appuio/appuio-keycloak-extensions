package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupToAttributeMapperConfigTest {

    @Test
    void testGetIgnoreGroupsList_GivenStringWith2Entries_ThenSplitIn2() {
        var map = Collections.singletonMap(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, "sapphire-stars##rose-canyon");
        var config = new GroupToAttributeMapper.MapperConfig(map);

        var ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).containsExactly("sapphire-stars", "rose-canyon");
    }

    @Test
    void testGetIgnoreGroupsList_GivenSingleEntry_ThenReturnSingleEntry() {
        var map = Collections.singletonMap(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, "sapphire-stars");
        var config = new GroupToAttributeMapper.MapperConfig(map);

        var ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).containsExactly("sapphire-stars");
    }

    @Test
    void testGetIgnoreGroupsList_GivenEmptyEntry_ThenReturnNothing() {
        var map = Collections.singletonMap(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, "");
        var config = new GroupToAttributeMapper.MapperConfig(map);

        var ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).isEmpty();
    }

    @Test
    void testGetIgnoreGroupsList_GivenNullEntry_ThenReturnNothing() {
        Map<String, String> map = Collections.emptyMap();
        var config = new GroupToAttributeMapper.MapperConfig(map);

        var ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).isEmpty();
    }
}
