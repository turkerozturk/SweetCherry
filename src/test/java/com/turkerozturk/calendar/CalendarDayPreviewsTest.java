package com.turkerozturk.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class CalendarDayPreviewsTest {
    CalendarSettings settings=new CalendarSettings("MONDAY","UTC",10000);
    CalendarDayPreviews previews=new CalendarDayPreviews(settings);
    @Test void unicodeCharactersAndSourceLinesAreBoundedWithoutSplittingEmoji(){
        ReflectionTestUtils.setField(settings,"previewCharacters",4);ReflectionTestUtils.setField(settings,"previewLines",2);
        assertThat(previews.preview("plain-text","🍒🍒🍒🍒🍒",new long[4]).text()).isEqualTo("🍒🍒🍒…");
        ReflectionTestUtils.setField(settings,"previewCharacters",100);
        assertThat(previews.preview("plain-text","one\r\ntwo\rthree",new long[4]).text()).isEqualTo("one\ntwo…");
        ReflectionTestUtils.setField(settings,"previewCharacters",1);
        assertThat(previews.preview("plain-text","large",new long[4]).text()).isEqualTo("…");
    }
    @Test void markupProducesOnlyReadableTextAndCountsItsImagesAndTables(){
        var html=previews.preview("html","<p>Hello <b>world</b></p><img src='data:image/png;base64,AA=='><table><tr><td>cell</td></tr></table><script>alert(1)</script>",new long[4]);
        assertThat(html.text()).contains("Hello world","cell").doesNotContain("<p>","base64","alert(1)","\n\n\n");
        assertThat(html.tables()).isEqualTo(1);
        var markdown=previews.preview("markdown","# Title\n\nA **bold** note",new long[4]);assertThat(markdown.text()).contains("Title","bold").doesNotContain("**");
    }
    @Test void richTextRejectsEntitiesAndMalformedXmlButReadsABoundedPrefix(){
        assertThat(previews.preview("custom-colors","<!DOCTYPE node [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><node><rich_text>&x;</rich_text></node>",new long[4]).unavailable()).isTrue();
        assertThat(previews.preview("custom-colors","<node><rich_text>broken",new long[4]).unavailable()).isTrue();
        var prefix=previews.preview("custom-colors","<node><rich_text>"+"a".repeat(70000)+"</rich_text></node>",new long[4]);
        assertThat(prefix.unavailable()).isFalse();assertThat(prefix.text()).hasSize(600).endsWith("…");
        assertThat(previews.preview("custom-colors","",new long[4]).text()).isEmpty();
    }
}
