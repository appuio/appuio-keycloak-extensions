package cloud.appuio.keycloak.extensions.mappers;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultOrganizationMapperConfigTest {

    @Test
    void testGetIgnoreGroupsList_GivenStringWith2Entries_ThenSplitIn2() {
        Map<String, String> map = Collections.singletonMap(DefaultOrganizationMapper.IGNORE_GROUPS_PROPERTY, "sapphire-stars##rose-canyon");
        DefaultOrganizationMapper.MapperConfig config = new DefaultOrganizationMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).containsExactly("sapphire-stars", "rose-canyon");
    }

    @Test
    void testGetIgnoreGroupsList_GivenSingleEntry_ThenReturnSingleEntry() {
        Map<String, String> map = Collections.singletonMap(DefaultOrganizationMapper.IGNORE_GROUPS_PROPERTY, "sapphire-stars");
        DefaultOrganizationMapper.MapperConfig config = new DefaultOrganizationMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).containsExactly("sapphire-stars");
    }

    @Test
    void testGetIgnoreGroupsList_GivenEmptyEntry_ThenReturnNothing() {
        Map<String, String> map = Collections.singletonMap(DefaultOrganizationMapper.IGNORE_GROUPS_PROPERTY, "");
        DefaultOrganizationMapper.MapperConfig config = new DefaultOrganizationMapper.MapperConfig(map);

        Stream<String> ignoreGroupList = config.getIgnoreGroups();

        assertThat(ignoreGroupList).isEmpty();
    }
}
