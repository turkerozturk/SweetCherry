package com.turkerozturk.export.texttree;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="myapp.text-tree-export")
public class TextTreeExportLimits {
    private int maxNodes=10000,maxDepth=64,maxNodeCharacters=8*1024*1024;
    private long maxTextBytes=64L*1024*1024;
    public int getMaxNodes(){return maxNodes;} public void setMaxNodes(int n){maxNodes=positive(n);}
    public int getMaxDepth(){return maxDepth;} public void setMaxDepth(int n){maxDepth=positive(n);}
    public int getMaxNodeCharacters(){return maxNodeCharacters;} public void setMaxNodeCharacters(int n){maxNodeCharacters=positive(n);}
    public long getMaxTextBytes(){return maxTextBytes;} public void setMaxTextBytes(long n){if(n<1)throw new IllegalArgumentException("Text export limits must be positive");maxTextBytes=n;}
    private int positive(int n){if(n<1)throw new IllegalArgumentException("Text export limits must be positive");return n;}
}
