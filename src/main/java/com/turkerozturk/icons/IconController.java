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
package com.turkerozturk.icons;

import com.turkerozturk.helpers.NodeIcon;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class IconController {
    private final MessageSource messages;
    public IconController(MessageSource messages){this.messages=messages;}
    public record Options(String sort,String dir,String q,String category){
        public Options{
            q=q==null?"":q.strip();category=category==null?"":category;
            if(!Set.of("index","name","category").contains(sort)||!Set.of("asc","desc").contains(dir)||q.length()>200||category.length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
    public record Row(int index,NodeIcon icon,String categoryLabel){}
    public record Category(String key,String label){}
    public record Listing(List<Row> rows,List<Category> categories,Options options,int total){}

    @GetMapping("/icons")
    public String listAllIcons(Model model,Locale locale,
            @RequestParam(defaultValue="category")String sort,@RequestParam(defaultValue="asc")String dir,
            @RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String category){
        model.addAttribute("icons",listing(new Options(sort,dir,q,category),locale));return "icons";
    }
    Listing listing(Options options,Locale locale){
        var collator=Collator.getInstance(locale);
        Comparator<String> text=collator::compare;
        var categories=Arrays.stream(NodeIcon.values()).map(NodeIcon::getCategory).distinct()
                .map(key->new Category(key,messages.getMessage("icons.category."+key,null,key,locale)))
                .sorted(Comparator.comparing(Category::label,text).thenComparing(Category::key)).toList();
        if(!options.category().isEmpty()&&categories.stream().noneMatch(c->c.key().equals(options.category())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var labels=categories.stream().collect(Collectors.toMap(Category::key,Category::label));
        String query=searchKey(options.q());
        var rows=Arrays.stream(NodeIcon.values()).map(icon->new Row(icon.ordinal()+1,icon,labels.get(icon.getCategory())))
                .filter(row->options.category().isEmpty()||row.icon().getCategory().equals(options.category()))
                .filter(row->query.isEmpty()||searchKey(row.icon().getIconNameForHuman()+" "+row.icon().name()+" "+row.icon().getIconName()+" "+row.icon().getCategory()+" "+row.categoryLabel()).contains(query))
                .collect(Collectors.toCollection(ArrayList::new));
        Comparator<Row> primary=switch(options.sort()){
            case "index"->Comparator.comparingInt(Row::index);
            case "name"->Comparator.comparing(r->r.icon().getIconNameForHuman(),text);
            default->Comparator.comparing(Row::categoryLabel,text);
        };
        if(options.dir().equals("desc"))primary=primary.reversed();
        rows.sort(primary.thenComparing(r->r.icon().getIconNameForHuman(),text).thenComparingInt(Row::index));
        return new Listing(List.copyOf(rows),categories,options,NodeIcon.values().length);
    }
    private static String searchKey(String value){return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('ı','i');}
    public LinkedHashSet<NodeIcon> getAllIcons(){return listing(new Options("category","asc","",""),Locale.ENGLISH).rows().stream().map(Row::icon).collect(Collectors.toCollection(LinkedHashSet::new));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Void> invalid(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).build();}
}
