package com.turkerozturk.image.attachments.preview;

import org.springframework.context.i18n.LocaleContextHolder;
import java.util.ResourceBundle;

public final class FileAttachmentLinks {
    private FileAttachmentLinks() { }
    public static String label(){return ResourceBundle.getBundle("messages",LocaleContextHolder.getLocale()).getString("filepreview.open");}
}
