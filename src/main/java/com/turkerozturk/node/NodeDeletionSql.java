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
package com.turkerozturk.node;

import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Deletes occurrence subtrees, preserving content when a shared group survives outside them. */
final class NodeDeletionSql {
    private NodeDeletionSql() { }
    private record Row(long id,long parent,long master,long sequence) { }

    static long delete(Connection connection,long selectedId) throws SQLException {
        var rows=new LinkedHashMap<Long,Row>();
        var contentIds=new HashSet<Long>();
        try(var statement=connection.createStatement();var result=statement.executeQuery(
                "SELECT node_id,father_id,COALESCE(master_id,0),sequence FROM children ORDER BY node_id")) {
            while(result.next()) {
                var row=new Row(result.getLong(1),result.getLong(2),result.getLong(3),result.getLong(4));
                if(row.id<=0 || rows.put(row.id,row)!=null) throw conflict();
            }
        }
        if(!rows.containsKey(selectedId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Node not found");
        try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT node_id FROM node")) {
            while(result.next()) contentIds.add(result.getLong(1));
        }
        var byParent=new HashMap<Long,List<Long>>();
        var aliases=new HashMap<Long,List<Long>>();
        var checked=new HashSet<Long>();
        for(var row:rows.values()) {
            if(row.master==0 ? !contentIds.contains(row.id)
                    : contentIds.contains(row.id) || !contentIds.contains(row.master)
                    || !rows.containsKey(row.master) || rows.get(row.master).master!=0) throw conflict();
            var path=new HashSet<Long>();long current=row.id;
            while(current!=0 && !checked.contains(current)) {
                var ancestor=rows.get(current);
                if(ancestor==null || !path.add(current)) throw conflict();
                current=ancestor.parent;
            }
            checked.addAll(path);
            byParent.computeIfAbsent(row.parent,key->new ArrayList<>()).add(row.id);
            if(row.master!=0) aliases.computeIfAbsent(row.master,key->new ArrayList<>()).add(row.id);
        }
        var deleting=new LinkedHashSet<Long>();var pending=new ArrayDeque<Long>();pending.add(selectedId);
        while(!pending.isEmpty()) {
            long id=pending.removeFirst();
            if(!deleting.add(id)) throw conflict();
            pending.addAll(byParent.getOrDefault(id,List.of()));
        }
        // Pick a surviving occurrence in the same transaction as deletion.
        var selected=rows.get(selectedId);
        var siblings=rows.values().stream().filter(row->row.parent==selected.parent)
                .sorted(Comparator.comparingLong(Row::sequence).thenComparingLong(Row::id)).toList();
        int at=siblings.indexOf(selected);
        long nextSelected=at>0 ? siblings.get(at-1).id
                : at+1<siblings.size() ? siblings.get(at+1).id : selected.parent;
        // Decide all promotions before changing any row; references are not hierarchy edges.
        var promotions=new LinkedHashMap<Long,Long>();
        for(long id:deleting) if(rows.get(id).master==0) {
            aliases.getOrDefault(id,List.of()).stream().filter(alias->!deleting.contains(alias))
                    .findFirst().ifPresent(alias->promotions.put(id,alias));
        }
        for(var promotion:promotions.entrySet()) {
            long original=promotion.getKey(), replacement=promotion.getValue();
            // Rekey payloads in place: preserve every column, SQL NULL and binary object exactly.
            for(String table:List.of("node","image","grid","codebox")) {
                try(var update=connection.prepareStatement("UPDATE "+table+" SET node_id=? WHERE node_id=?")) {
                    update.setLong(1,replacement);update.setLong(2,original);
                    if(update.executeUpdate()!=1 && table.equals("node")) throw conflict();
                }
            }
            try(var update=connection.prepareStatement("UPDATE children SET master_id=? WHERE master_id=?")) {
                update.setLong(1,replacement);update.setLong(2,original);update.executeUpdate();
            }
            try(var update=connection.prepareStatement("UPDATE children SET master_id=0 WHERE node_id=?")) {
                update.setLong(1,replacement);if(update.executeUpdate()!=1) throw conflict();
            }
        }
        var reverse=new ArrayList<>(deleting);Collections.reverse(reverse);
        for(long id:reverse) {
            for(String table:List.of("bookmark","codebox","image","grid","children","node")) {
                try(var delete=connection.prepareStatement("DELETE FROM "+table+" WHERE node_id=?")) {
                    delete.setLong(1,id);delete.executeUpdate();
                }
            }
        }
        return nextSelected;
    }
    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,"Invalid tree or shared content reference; deletion was cancelled.");
    }
}
