package com.turkerozturk.export.texttree;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SymbolExportNamesTest {
    @Test void markerRemovalPrecedesCaseInsensitiveCollisionAllocationAndDirectoryFallbacks() {
        var paths=new TextTreePaths();
        assertThat(paths.allocate("","Report{{fa-solid fa-image}}",1,false)).isEqualTo("Report.txt");
        assertThat(paths.allocate("","report{{ctb:ct_camera}}",2,false)).isEqualTo("report--2.txt");
        assertThat(paths.allocate("","{{fa-brands fa-github}}",3,true)).isEqualTo("node-3");
        assertThat(paths.allocate("","{{record.name}}",4,false)).isEqualTo("{{record.name}}");
    }
}
