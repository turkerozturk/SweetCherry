package com.turkerozturk.image.attachments.drawio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Resource budgets shared by the attachment loader, parser and browser contract. */
@Component
public record DrawioSettings(int fileBytes, int pageBytes, int totalBytes, int pages, int cells, int totalCells, int labelCharacters, int xmlElements, int xmlAttributes, int xmlDepth, int parentDepth, int geometryDepth, int geometryPoints, int coordinate) {
    public DrawioSettings(
            @Value("${myapp.drawio-preview.max-file-bytes:16777216}") int fileBytes,
            @Value("${myapp.drawio-preview.max-page-bytes:16777216}") int pageBytes,
            @Value("${myapp.drawio-preview.max-total-bytes:134217728}") int totalBytes,
            @Value("${myapp.drawio-preview.max-pages:512}") int pages,
            @Value("${myapp.drawio-preview.max-cells-per-page:20000}") int cells,
            @Value("${myapp.drawio-preview.max-total-cells:200000}") int totalCells,
            @Value("${myapp.drawio-preview.max-label-characters:100000}") int labelCharacters,
            @Value("${myapp.drawio-preview.max-xml-elements:500000}") int xmlElements,
            @Value("${myapp.drawio-preview.max-xml-attributes:128}") int xmlAttributes,
            @Value("${myapp.drawio-preview.max-xml-depth:128}") int xmlDepth,
            @Value("${myapp.drawio-preview.max-parent-depth:256}") int parentDepth,
            @Value("${myapp.drawio-preview.max-geometry-depth:16}") int geometryDepth,
            @Value("${myapp.drawio-preview.max-geometry-points:10000}") int geometryPoints,
            @Value("${myapp.drawio-preview.max-coordinate:1000000}") int coordinate) {
        if(fileBytes<1||fileBytes>67108864)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-file-bytes");
        this.fileBytes=fileBytes;
        if(pageBytes<1||pageBytes>67108864)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-page-bytes");
        this.pageBytes=pageBytes;
        if(totalBytes<1||totalBytes>268435456)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-total-bytes");
        this.totalBytes=totalBytes;
        if(pages<1||pages>2000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-pages");
        this.pages=pages;
        if(cells<1||cells>50000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-cells-per-page");
        this.cells=cells;
        if(totalCells<1||totalCells>500000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-total-cells");
        this.totalCells=totalCells;
        if(labelCharacters<1||labelCharacters>1000000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-label-characters");
        this.labelCharacters=labelCharacters;
        if(xmlElements<1||xmlElements>2000000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-xml-elements");
        this.xmlElements=xmlElements;
        if(xmlAttributes<1||xmlAttributes>1024)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-xml-attributes");
        this.xmlAttributes=xmlAttributes;
        if(xmlDepth<1||xmlDepth>256)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-xml-depth");
        this.xmlDepth=xmlDepth;
        if(parentDepth<1||parentDepth>512)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-parent-depth");
        this.parentDepth=parentDepth;
        if(geometryDepth<1||geometryDepth>32)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-geometry-depth");
        this.geometryDepth=geometryDepth;
        if(geometryPoints<1||geometryPoints>50000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-geometry-points");
        this.geometryPoints=geometryPoints;
        if(coordinate<1||coordinate>10000000)throw new IllegalArgumentException("Invalid myapp.drawio-preview.max-coordinate");
        this.coordinate=coordinate;
    }
    public static DrawioSettings defaults(){return new DrawioSettings(16777216,16777216,134217728,512,20000,200000,100000,500000,128,128,256,16,10000,1000000);}
}
