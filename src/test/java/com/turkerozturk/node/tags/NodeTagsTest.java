package com.turkerozturk.node.tags;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class NodeTagsTest {
 @Test void normalizesOnlyAsciiSpacesAndExactDuplicatesPreservingOrderCaseAndUnicode(){
  assertThat(NodeTags.normalize("  Türkçe   iş Tag tag Türkçe C++ <x> a&b  ")).isEqualTo("Türkçe iş Tag tag C++ <x> a&b");
  assertThat(NodeTags.split("a\tb a\tb foo\u00A0bar")).containsExactly("a\tb","foo\u00A0bar");
  assertThat(NodeTags.normalize("   ")).isEmpty();assertThat(NodeTags.split(null)).isEmpty();
 }
 @Test void rejectsOversizeAndNulWithoutIntroducingPunctuationRules(){
  assertThatThrownBy(()->NodeTags.normalize("x".repeat(20001))).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->NodeTags.normalize("a\0b")).isInstanceOf(IllegalArgumentException.class);
  assertThat(NodeTags.normalize("#tag 100% a+b x/y 😀")).isEqualTo("#tag 100% a+b x/y 😀");
 }
}
