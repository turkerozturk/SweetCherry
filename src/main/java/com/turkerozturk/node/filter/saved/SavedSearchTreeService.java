package com.turkerozturk.node.filter.saved;

import com.turkerozturk.node.filter.AdvancedNodeSearch;
import com.turkerozturk.node.filter.FormSearch;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.server.ResponseStatusException;

/** Runs a user's existing preset through the same validated, database-side search. */
@Service
public class SavedSearchTreeService {
    private final SavedFiltersStore store;
    private final AdvancedNodeSearch search;
    private final Validator validator;
    public SavedSearchTreeService(SavedFiltersStore store, AdvancedNodeSearch search, Validator validator) {
        this.store=store;this.search=search;this.validator=validator;
    }
    public record Branch(String id,String name) {}
    public record Result(long nodeId,String name,String iconName,String titleColor,boolean bold,
            boolean hasTags,String tags,boolean hasChildren,String path) {}
    public record Results(String id,String name,int page,long total,boolean hasNext,List<Result> results) {}
    public List<Branch> list(String scope)throws IOException {
        return store.read(scope).presets().stream().map(p->new Branch(p.id(),p.name())).toList();
    }
    public Results results(String scope,String id,int page)throws IOException {
        if(id==null||!id.matches("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}")||page<0||page>1000000)
            throw new IllegalArgumentException("Invalid saved search request");
        var preset=store.read(scope).presets().stream().filter(p->p.id().equals(id)).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var form=form(preset.fields(),page);
        var found=search.search(form);
        var results=found.getContent().stream().map(n->new Result(n.getNodeId(),n.getName(),
                n.getNodeIcon()==null?"zero":n.getNodeIcon().getIconName(),n.getTitleColorAsHtmlHex(),n.isBoldnessBit(),
                n.getTags()!=null&&!n.getTags().isBlank(),n.getTags(),false,
                n.getBreadcrumbs()==null?"":String.join(" / ",n.getBreadcrumbs().values()))).toList();
        return new Results(id,preset.name(),found.getNumber(),found.getTotalElements(),found.hasNext(),results);
    }
    FormSearch form(Map<String,List<String>> fields,int page) {
        var form=new FormSearch();var binder=new WebDataBinder(form);
        binder.setAllowedFields(SavedFiltersStore.FIELDS.stream().filter(key->!key.equals("pageSize")).toArray(String[]::new));
        binder.setConversionService(new DefaultFormattingConversionService());
        var values=new MutablePropertyValues();
        fields.forEach((key,list)->{if(!key.equals("pageSize")&&!list.isEmpty())values.add(key,list.toArray(String[]::new));});
        binder.bind(values);form.setPageSize(50);form.setPageNumber(page);
        if(binder.getBindingResult().hasErrors()||!validator.validate(form).isEmpty())
            throw new IllegalArgumentException("Invalid saved search fields");
        return form;
    }
}
