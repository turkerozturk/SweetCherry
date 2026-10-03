package com.turkerozturk.help;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HelpControllerTest {
    @Test void removedHelpPagesRedirectToMaintainedDocumentation() {
        var controller = new HelpController();
        assertThat(controller.getIndex()).isEqualTo("redirect:https://github.com/turkerozturk/SweetCherry#readme");
        assertThat(controller.helpPdf()).isEqualTo("redirect:/help/cherrytemplatenode");
    }
}
