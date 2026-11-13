package com.turkerozturk.settings.editing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Application-wide appearance, evaluated on the server before any page is painted. */
@ControllerAdvice
public class UiThemeAdvice {
    private final String theme;
    public UiThemeAdvice(@Value("${myapp.appearance.theme:light}") String theme) {
        this.theme="dark".equalsIgnoreCase(theme.strip())?"dark":"light";
    }
    @ModelAttribute("uiTheme") public String theme(){return theme;}
}
