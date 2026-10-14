package com.turkerozturk.node.filter;

import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.NodeService;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashSet;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class IconFilterOptionsTest {
    @Test void noIconIsFirstAndOtherNamesAreAlphabetical() {
        var icons = new FormSearch().getAllIcons().stream().toList();
        assertThat(icons.get(0)).isEqualTo(NodeIcon.OTHER);
        var labels = icons.subList(1,icons.size()).stream().map(NodeIcon::getIconNameForHuman).toList();
        assertThat(labels).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(icons).containsExactlyInAnyOrder(NodeIcon.values());
    }
    @Test void mixedNoIconAndExplicitIconIncludesReadOnlyVariants() {
        var icons = new LinkedHashSet<>(List.of(NodeIcon.OTHER,NodeIcon.CT_DONE));
        assertThat(NodeService.iconFilterValues(icons)).containsExactly(0,1,14,15);
        assertThat(NodeService.iconFilterValues(new LinkedHashSet<>())).isEmpty();
    }
}
