package com.turkerozturk.image.gallery;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice(assignableTypes=GalleryController.class)
public class ImageAnnotationAdvice {
    private final NodePropertiesService properties;
    public ImageAnnotationAdvice(NodePropertiesService properties){this.properties=properties;}
    @ModelAttribute("galleryCanAnnotate") public boolean writable(){return properties.writable();}
}
