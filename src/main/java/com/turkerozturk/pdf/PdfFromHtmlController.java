/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.pdf;

import com.turkerozturk.helpers.DateTimeHelper;
import com.turkerozturk.helpers.PDFFromHTMLHelper;
import com.turkerozturk.node.*;
import com.turkerozturk.node.filter.FormSearch;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;

@RestController
public class PdfFromHtmlController {

    private TemplateService templateService;
    private final TemplateEngine templateEngine;
    private final PDFFromHTMLHelper pdfFromHTMLHelper;
    private final MessageSource messageSource;
    private final ResourceLoader resourceLoader;
    private NodeService nodeService;

    private NodeContentParserService nodeContentParserService;

    @Autowired
    public PdfFromHtmlController(PDFFromHTMLHelper pdfFromHTMLHelper, ResourceLoader resourceLoader,
                                 TemplateService templateService, NodeService nodeService,
                                 NodeContentParserService nodeContentParserService,
                                 MessageSource messageSource1) {
        this.pdfFromHTMLHelper = pdfFromHTMLHelper;
        this.resourceLoader = resourceLoader;
        this.messageSource = messageSource1;
        this.templateEngine = initializeTemplateEngine();
        this.templateService = templateService;
        this.nodeService = nodeService;
        this.nodeContentParserService = nodeContentParserService;

    }

    @GetMapping("/export-node-to-pdf/{nodeId}")
    public ResponseEntity<byte[]> exportNodeAsPdf(@PathVariable long nodeId, HttpServletRequest request) {

        // DATA (we are not using it for now, will use later)
        Node node = nodeService.getById(nodeId);
        // TEMPLATE ENGINE
        Context context = new Context();
        node = nodeContentParserService.parseNodeContent(node, request);

        String txt = nodeContentParserService.parseNodeTxt(node, true);
        node.setTxtAsHtml(txt);

        context.setVariable( "newlinechar" , '\n' ) ;


        context.setVariable("node", node);

        String sourceHTMLContent = templateEngine.process("node/exportNodeToPdf", context);

        // STRING HTML TO BYTE ARRAY PDF CONVERSION
        byte[] pdfBytes = pdfFromHTMLHelper.createPdfFromHtml(sourceHTMLContent);

        // PREPARATION FOR PDF FILE DOWNLOAD
        String targetPdfFileName = node.getName();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", targetPdfFileName.replace(" ",""));
        headers.setContentLength(pdfBytes.length);

        // SERVING PDF FILE
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/cherrytemplatenode/pdf/{templateNodeId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable long templateNodeId) {

        // DATA (we are not using it for now, will use later)
        TemplateNode templateNode = templateService.getTemplateNode(templateNodeId);

        // TEMPLATE ENGINE
        Context context = new Context();
        context.setVariable("degisken1", "Bu metin bir Thymeleaf Context değişkeninin, " +
                "Tyhmeleaf Template Engine tarafından process edilmesiyle HTML şablonda yerine yerleşti.");

        context.setVariable("cherryTemplateNode", templateNode);

        String sourceHTMLContent = templateEngine.process("cherrytemplatenode-pdf", context);

        // STRING HTML TO BYTE ARRAY PDF CONVERSION
        byte[] pdfBytes = pdfFromHTMLHelper.createPdfFromHtml(sourceHTMLContent);

        // PREPARATION FOR PDF FILE DOWNLOAD
        String targetPdfFileName = templateNode.getName();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", targetPdfFileName);
        headers.setContentLength(pdfBytes.length);

        // SERVING PDF FILE
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private TemplateEngine initializeTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

// bilgi yenisi bu TODO
    @PostMapping("/nodesadvancedwithbinding/pdf")
    public ResponseEntity<byte[]> pdfAllNodesAdvancedAsHtml(@Valid FormSearch formSearch, BindingResult bindingResult, Model model) {


        // TEMPLATE ENGINE
        Context context = new Context();

        context.setVariable("nodes", nodeService.getFilteredFormNodes(formSearch, bindingResult));

        // Get message from MessageSource
        String lastModifiedDate = messageSource.getMessage("bookmarks.grid.th.lastModifiedDate", null, LocaleContextHolder.getLocale());
        context.setVariable("lastModifiedDate", lastModifiedDate);

        String sourceHTMLContent = templateEngine.process("filterednodes-pdf", context);

        // STRING HTML TO BYTE ARRAY PDF CONVERSION
        byte[] pdfBytes = pdfFromHTMLHelper.createPdfFromHtml(sourceHTMLContent);

        // PREPARATION FOR PDF FILE DOWNLOAD
        String targetPdfFileName = "nodesadvanced.pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", targetPdfFileName);
        headers.setContentLength(pdfBytes.length);
        System.out.println(sourceHTMLContent);
        // SERVING PDF FILE
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

    }


    // bilgi: eskisi bu
    @GetMapping("/nodesadvanced/pdf")
    public ResponseEntity<byte[]> pdfAllNodesAdvancedAsHtml(
                                            @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                            @RequestParam(required = false, defaultValue = "30") int pageSize,
                                            @RequestParam(required = false) Long nodeIcons,
                                            @RequestParam(required = false) String tagsFilter,
                                            @RequestParam(required = false) boolean nameStartsWith,
                                            @RequestParam(required = false) String nameFilter,
                                            @RequestParam(required = false) String textFilter,
                                            @RequestParam(required = false) String tsCreationStartFilter,
                                            @RequestParam(required = false) String tsCreationEndFilter,
                                            @RequestParam(required = false) String tsModificationStartFilter,
                                            @RequestParam(required = false) String tsModificationEndFilter
    ) {

        String escNameFilter = "";
        if (nameFilter != null && !nameFilter.isEmpty()) {
            if (nameStartsWith) {
            } else {
            }
            escNameFilter = nameFilter;
        }

        String escTagsFilter = "";
        if (tagsFilter != null && !tagsFilter.isEmpty()) {
            escTagsFilter = tagsFilter;
        }

        String escTextFilter = "";
        if (textFilter != null && !textFilter.isEmpty()) {
            escTextFilter = textFilter;
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize);//, Sort.by("name").descending());

        final String nowAsString = DateTimeHelper.formatDate(LocalDate.now(), "yyyy-MM-dd");
        final String oneWeekAgoAsString = DateTimeHelper.formatDate(LocalDate.now().minusDays(7), "yyyy-MM-dd");

        if(tsCreationStartFilter == null || tsCreationStartFilter.isEmpty()) {
            tsCreationStartFilter = oneWeekAgoAsString; //"1970-01-01"
        }
        if(tsCreationEndFilter == null || tsCreationEndFilter.isEmpty()) {
            tsCreationEndFilter = nowAsString;//"2038-01-19";
        }
        long tsCreationStart = DateTimeHelper.convertToUnixTimestamp(tsCreationStartFilter);
        long tsCreationEnd = DateTimeHelper.convertToUnixTimestamp(tsCreationEndFilter);

        if(tsModificationStartFilter == null || tsModificationStartFilter.isEmpty()) {
            tsModificationStartFilter = oneWeekAgoAsString;
        }
        if(tsModificationEndFilter == null || tsModificationEndFilter.isEmpty()) {
            tsModificationEndFilter = nowAsString;//"2038-01-19";
        }
        long tsModificationStart = DateTimeHelper.convertToUnixTimestamp(tsModificationStartFilter);
        long tsModificationEnd = DateTimeHelper.convertToUnixTimestamp(tsModificationEndFilter);

        Page<Node> nodes;
        if(nodeIcons == null) {
            //nodeIcons = 0L;
            nodes = nodeService.getNodesWithCustomQuery(escNameFilter, escTextFilter, escTagsFilter,
                    tsCreationStart, tsCreationEnd, tsModificationStart, tsModificationEnd, pageable);
        } else {
            nodes = nodeService.getNodesWithCustomQuery(nodeIcons, escNameFilter, escTextFilter, escTagsFilter,
                    tsCreationStart, tsCreationEnd, tsModificationStart, tsModificationEnd, pageable);
        }

        /*

        NodeIcon[] sortedIcons = NodeIcon.values();
        Arrays.sort(sortedIcons, Comparator.comparing(NodeIcon::getIconNameForHuman));

        model.addAttribute("nodeIcons", sortedIcons);
        model.addAttribute("nodes", nodes);
        model.addAttribute("nameFilter", nameFilter);
        model.addAttribute("tagsFilter", tagsFilter);
        model.addAttribute("textFilter", textFilter);
        model.addAttribute("tsCreationStartFilter", tsCreationStartFilter);
        model.addAttribute("tsCreationEndFilter", tsCreationEndFilter);
        model.addAttribute("pageSizeOptions", PAGE_SIZE_OPTIONS);

        */

        // TEMPLATE ENGINE
        Context context = new Context();

        context.setVariable("nodes", nodes);

        // Get message from MessageSource
        String lastModifiedDate = messageSource.getMessage("bookmarks.grid.th.lastModifiedDate", null, LocaleContextHolder.getLocale());
        context.setVariable("lastModifiedDate", lastModifiedDate);

        String sourceHTMLContent = templateEngine.process("filterednodes-pdf", context);

        // STRING HTML TO BYTE ARRAY PDF CONVERSION
        byte[] pdfBytes = pdfFromHTMLHelper.createPdfFromHtml(sourceHTMLContent);

        // PREPARATION FOR PDF FILE DOWNLOAD
        String targetPdfFileName = "nodesadvanced.pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", targetPdfFileName);
        headers.setContentLength(pdfBytes.length);
        System.out.println(sourceHTMLContent);
        // SERVING PDF FILE
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

    }

}
