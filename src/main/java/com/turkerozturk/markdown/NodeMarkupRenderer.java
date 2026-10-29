package com.turkerozturk.markdown;
/** Node-level source-to-safe-HTML contract; embedded codeboxes never use this registry. */
public interface NodeMarkupRenderer {
    String syntax();
    String render(String source);
}
