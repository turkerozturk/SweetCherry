package com.turkerozturk.node.fragments;

import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class NodeFragmentAdvice {
    private final NodeFragmentSettings settings;
    public NodeFragmentAdvice(NodeFragmentSettings settings){this.settings=settings;}
    @ModelAttribute("nodeFragmentDepth") public int depth(){return settings.depth();}
    @ModelAttribute("nodeFragmentInclusions") public int inclusions(){return settings.inclusions();}
}
