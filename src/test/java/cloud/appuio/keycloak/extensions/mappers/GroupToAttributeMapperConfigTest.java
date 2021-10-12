package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupToAttributeMapperConfigTest {

    @Test
    void testGetIgnoreGroupsList_GivenStringWith2Entries_ThenSplitIn2() {
        Map<String, String> map = Collections.singletonMap(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, "sapphire-stars##rose-canyon");
        GroupToAttributeMapper.MapperConfig config = new GroupToAttributeMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).containsExactly("sapphire-stars", "rose-canyon");
    }

    @Test
    void testGetIgnoreGroupsList_GivenSingleEntry_ThenReturnSingleEntry() {
        Map<String, String> map = Collections.singletonMap(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, "sapphire-stars");
        GroupToAttributeMapper.MapperConfig config = new GroupToAttributeMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).containsExactly("sapphire-stars");
    }

    @Test
    void testGetIgnoreGroupsList_GivenEmptyEntry_ThenReturnNothing() {
        Map<String, String> map = Collections.singletonMap(GroupToAttributeMapper.IGNORE_GROUPS_PROPERTY, "");
        GroupToAttributeMapper.MapperConfig config = new GroupToAttributeMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).isEmpty();
    }

    @Test
    void testGetIgnoreGroupsList_GivenNullEntry_ThenReturnNothing() {
        Map<String, String> map = Collections.emptyMap();
        GroupToAttributeMapper.MapperConfig config = new GroupToAttributeMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).isEmpty();
    }
}
