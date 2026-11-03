package com.turkerozturk.customlists;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice
public class CustomListsAdvice {
    private final CustomListsSettings settings;
    public CustomListsAdvice(CustomListsSettings settings){this.settings=settings;}
    @ModelAttribute("customListNames") public CustomListsSettings.Names names(){return settings.effective();}
}
