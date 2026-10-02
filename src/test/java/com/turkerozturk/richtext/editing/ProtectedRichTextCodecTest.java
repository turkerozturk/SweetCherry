package com.turkerozturk.richtext.editing;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.turkerozturk.richtext.experimental.*;
import com.turkerozturk.richtext.editing.ProtectedRichTextCodec.Reference;

class ProtectedRichTextCodecTest {
    private final ProtectedRichTextCodec codec = new ProtectedRichTextCodec();
    private final RichTextXmlReader reader = new RichTextXmlReader();
    private RichTextDocument.TextRun marker(String key, int offset) {
        return new RichTextDocument.TextRun("\uFFFC", Map.of("__sweet_object",key),offset);
    }
    @Test void opensTextAroundObjectAndRecalculatesOffsetAfterUnicodeInsertion() {
        var references=List.of(new Reference("image",1));
        var opened=codec.open(reader.read("<node><rich_text weight='heavy'>AB</rich_text></node>"),references);
        assertThat(opened.runs().stream().map(RichTextDocument.TextRun::text).reduce("",String::concat)).isEqualTo("A\uFFFCB");
        var edited=new RichTextDocument(List.of(new RichTextDocument.TextRun("😀A",Map.of("weight","heavy"),0),marker("image:1",2),new RichTextDocument.TextRun("B",Map.of(),3)));
        var saved=codec.save(edited,references);
        assertThat(saved.offsets()).containsEntry(references.get(0),2);
        assertThat(saved.text().runs().stream().map(RichTextDocument.TextRun::text).reduce("",String::concat)).isEqualTo("😀AB");
    }
    @Test void preservesAllTypesAndAdjacentObjects() {
        var refs=List.of(new Reference("image",0),new Reference("grid",1),new Reference("codebox",3));
        var doc=reader.read("<node><rich_text>X</rich_text></node>");
        var saved=codec.save(codec.open(doc,refs),refs);
        for(var ref:refs)assertThat(saved.offsets()).containsEntry(ref,ref.offset());
    }
    @Test void rejectsObjectDeletionDuplicationAndReordering() {
        var refs=List.of(new Reference("image",0),new Reference("grid",1));
        for(var runs:List.of(List.of(marker("image:0",0)),List.of(marker("image:0",0),marker("image:0",1)),List.of(marker("grid:1",0),marker("image:0",1)))) {
            assertThatThrownBy(()->codec.save(new RichTextDocument(runs),refs)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void rejectsObjectFormattingAndReservedSourceAttribute() {
        var refs=List.of(new Reference("image",0));
        var changed=new RichTextDocument(List.of(new RichTextDocument.TextRun("\uFFFC",Map.of("__sweet_object","image:0","weight","heavy"),0)));
        assertThatThrownBy(()->codec.save(changed,refs)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->codec.open(changed,refs)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void keepsEmptyRunsAndUnknownAttributesAroundObjects() {
        var doc=reader.read("<node><rich_text future='keep'/><rich_text future='keep'>AB</rich_text><rich_text/></node>");
        var refs=List.of(new Reference("image",1));
        var saved=codec.save(codec.open(doc,refs),refs);
        assertThat(saved.text().runs().stream().filter(run->run.text().isEmpty()).count()).isEqualTo(2);
        assertThat(saved.text().runs()).anySatisfy(run->assertThat(run.attributes()).containsEntry("future","keep"));
    }
    @Test void writerNeverSerializesEditorObjectMetadata() {
        var doc=reader.read("<node><rich_text>AB</rich_text></node>");
        var refs=List.of(new Reference("image",1));
        String xml=new RichTextXmlWriter().write(codec.save(codec.open(doc,refs),refs).text());
        assertThat(xml).doesNotContain("__sweet_object","\uFFFC");
    }
    @Test void insertsNewImageBetweenExistingObjectsWithoutChangingTheirOrder() {
        var refs = List.of(new Reference("image", 0), new Reference("grid", 1));
        var edited = new RichTextDocument(List.of(marker("image:0", 0), marker("new-image:test", 1), marker("grid:1", 2)));
        var saved = codec.save(edited, refs, java.util.Set.of("new-image:test"));
        assertThat(saved.offsets()).containsEntry(refs.get(0), 0).containsEntry(refs.get(1), 2);
        assertThat(saved.newImages()).containsEntry("new-image:test", 1);
        assertThat(new RichTextXmlWriter().write(saved.text())).doesNotContain("__sweet_object", "\uFFFC");
    }
    @Test void rejectsDuplicatedAndUnusedImagePayloads() {
        var keys = java.util.Set.of("new-image:test");
        assertThatThrownBy(() -> codec.save(new RichTextDocument(List.of(marker("new-image:test", 0), marker("new-image:test", 1))), List.of(), keys)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.save(new RichTextDocument(List.of()), List.of(), keys)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void placesAttachmentAndImageSlotsWithIndependentFinalOffsets() {
        var doc = new RichTextDocument(List.of(marker("new-file:test", 0), new RichTextDocument.TextRun("😀", Map.of(), 1), marker("new-image:test", 2)));
        var saved = codec.save(doc, List.of(), java.util.Set.of("new-file:test", "new-image:test"));
        assertThat(saved.newImages()).containsEntry("new-file:test", 0).containsEntry("new-image:test", 2);
        assertThat(saved.text().runs().get(0).text()).isEqualTo("😀");
    }
}
