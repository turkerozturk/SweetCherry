package com.turkerozturk.helpers;

import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CherryTreeIconIdentityTest {
 private List<String[]> fixture(String name)throws Exception{
  try(var input=getClass().getResourceAsStream("/fixtures/"+name)){
   return new String(Objects.requireNonNull(input).readAllBytes(),StandardCharsets.UTF_8).lines()
    .filter(line->!line.isBlank()&&!line.startsWith("#")).map(line->line.split("\t")).toList();
  }
 }
 @Test void all263SourceIdsAndBasenamesAreStableIncludingReadOnlyBit()throws Exception{
  var source=fixture("cherrytree-icon-identities.tsv");assertThat(source).hasSize(263);assertThat(NodeIcon.values()).hasSize(264);
  for(var entry:source){int id=Integer.parseInt(entry[0]);var icon=NodeIcon.getNodeIconByIconId(id);
   assertThat(icon.getIconName()).as("source ID %s",id).isEqualTo(entry[1]);assertThat(icon.getIconIdIn16bit()).isEqualTo(id*2);
   assertThat(NodeIcon.getNodeIconByIconIdIn16bit(id*2)).isSameAs(icon);
   for(int flag:new int[]{0,1}){var node=new Node();node.setSyntax("plain-text");node.setIsReadOnly16bit(id*2L+flag);node.parseNodeIconAndReadOnlyFlag();assertThat(node.getNodeIcon()).isSameAs(icon);}
  }
  assertThat(Arrays.stream(NodeIcon.values()).map(NodeIcon::getIconId)).doesNotHaveDuplicates();assertThat(Arrays.stream(NodeIcon.values()).map(NodeIcon::getIconName)).doesNotHaveDuplicates();
 }
 @Test void sourceCodeSyntaxMappingsRemainIdenticalWithoutInventingNewOnes()throws Exception{
  var source=fixture("cherrytree-code-icons.tsv");assertThat(source).hasSize(35);assertThat(CodeIcon.values()).hasSize(36);
  for(var entry:source)assertThat(CodeIcon.getCodeIconBySyntax(entry[0]).getNodeIcon().getIconName()).isEqualTo(entry[1]);
  assertThat(CodeIcon.getCodeIconBySyntax("sql").getNodeIcon()).isEqualTo(NodeIcon.CT_DB);
  assertThat(CodeIcon.getCodeIconBySyntax("vim")).isEqualTo(CodeIcon.OTHER);
 }
 @Test void newNamesRemainDiscoverableWithoutRenamingAssets(){
  assertThat(NodeIcon.fromNameOrLabel("ct_spring-boot")).isEqualTo(NodeIcon.CT_SPRING_BOOT);
  assertThat(NodeIcon.fromNameOrLabel("Spring Boot")).isEqualTo(NodeIcon.CT_SPRING_BOOT);
  assertThat(NodeIcon.fromNameOrLabel("Tamil")).isEqualTo(NodeIcon.CT_TA);
  assertThat(NodeIcon.CT_TA.getIconIdIn16bit()).isEqualTo(526);
 }
}
