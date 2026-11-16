package com.turkerozturk.image.attachments.word;

import org.springframework.context.i18n.LocaleContextHolder;
import java.util.ResourceBundle;

/** Shared label for the two rich-text renderers; inserted as escaped DOM text. */
public final class WordAttachmentLinks {
    private WordAttachmentLinks() {}
    public static String label() {
        return ResourceBundle.getBundle("messages", LocaleContextHolder.getLocale()).getString("word.open");
    }
}
