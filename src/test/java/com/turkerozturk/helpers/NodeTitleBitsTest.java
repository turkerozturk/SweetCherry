package com.turkerozturk.helpers;

import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeTitleBitsTest {
    @Test void packsColorBoldAndRichTextIntoTheirOwnBits() {
        assertThat(BitOperation.concatNodeTitleColorAndBoldnessAndTextType(0, false, false)).isZero();
        assertThat(BitOperation.concatNodeTitleColorAndBoldnessAndTextType(0, true, false)).isEqualTo(2);
        assertThat(BitOperation.concatNodeTitleColorAndBoldnessAndTextType(0, false, true)).isEqualTo(1);
        assertThat(BitOperation.concatNodeTitleColorAndBoldnessAndTextType(0x3584E4, true, true))
                .isEqualTo((0x3584E4L << 3) | 7L);
    }

    @Test void packedValueCanBeReadBackByNodeParser() {
        Node node = new Node();
        node.setIsRichText(BitOperation.concatNodeTitleColorAndBoldnessAndTextType(
                0x3584E4, true, true));

        node.parseNodeTitleColorAndBoldnessAndTextType();

        assertThat(node.getTitleColor()).isEqualTo(0x3584E4);
        assertThat(node.isBoldnessBit()).isTrue();
        assertThat(node.isRichTextBit()).isTrue();
    }
}
