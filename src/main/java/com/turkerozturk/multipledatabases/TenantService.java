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
package com.turkerozturk.multipledatabases;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CustomPropertiesHolder customPropertiesHolder;

    public Map<String, DataSource> getAllTenants() {
        AbstractRoutingDataSource routingDataSource = (AbstractRoutingDataSource) this.dataSource;
        Map<Object, DataSource> resolvedDataSources = routingDataSource.getResolvedDataSources();
        Map<String, DataSource> tenants = new HashMap<>();
        for (Map.Entry<Object, DataSource> entry : resolvedDataSources.entrySet()) {
            tenants.put((String) entry.getKey(), entry.getValue());

        }



        return tenants;
    }


    public void printDataSourceDetails() {
        AbstractRoutingDataSource routingDataSource = (AbstractRoutingDataSource) this.dataSource;
        Map<Object, DataSource> resolvedDataSources = routingDataSource.getResolvedDataSources();

        for (Map.Entry<Object, DataSource> entry : resolvedDataSources.entrySet()) {
            String tenantId = (String) entry.getKey();
            DataSource dataSource = entry.getValue();

            logger.info("Tenant ID: " + tenantId);

            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                logger.info("URL: " + hikariDataSource.getJdbcUrl());
                logger.info("Username: " + hikariDataSource.getUsername());
                logger.info("Password: " + hikariDataSource.getPassword());
                logger.info("Driver Class Name: " + hikariDataSource.getDriverClassName());
            } else {
                logger.info("DataSource is not an instance of HikariDataSource. Implement appropriate logic to extract information.");
            }

        }
    }


    public List<TenantForm> getTenantForms() {

        List<TenantForm> tenantForms = new ArrayList<>();

        AbstractRoutingDataSource routingDataSource = (AbstractRoutingDataSource) this.dataSource;
        Map<Object, DataSource> resolvedDataSources = routingDataSource.getResolvedDataSources();

        for (Map.Entry<Object, DataSource> entry : resolvedDataSources.entrySet()) {
            String tenantName = (String) entry.getKey();
            DataSource dataSource = entry.getValue();



            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                TenantForm tenantForm = new TenantForm();
                tenantForm.setDriverClassName(hikariDataSource.getDriverClassName());
                tenantForm.setTenantName(tenantName);
                tenantForm.setUrl(hikariDataSource.getJdbcUrl());
                tenantForm.setUsername(hikariDataSource.getUsername());
                tenantForm.setPassword(hikariDataSource.getPassword());
                //tenantForm.setFileName(hikariDataSource.);


                Map<String, String> customProperties = customPropertiesHolder.getCustomProperties(tenantName);

                tenantForm.setFileName(customProperties.get("propertyFileName"));

                //logger.info(String.valueOf(customProperties.size()));

                if(customProperties.containsKey("custom.isWritable")) {
                    String isWritableAsString = customProperties.get("custom.isWritable");
                    logger.info("isWritable: " + isWritableAsString + " (" + tenantName + ")");
                    boolean isWritable = isWritableAsString.equals("true");
                    tenantForm.setWritable(isWritable);

                    // model.addAttribute("isWritable", isWritable);
                }


                // model.addAttribute("propertyFileName", customProperties.get("propertyFileName"));

                // for(String s: customProperties.keySet()) {
                //    logger.info("customProperties key: " + s);
                //}

                tenantForms.add(tenantForm);



            } else {
                logger.info("DataSource is not an instance of HikariDataSource. Implement appropriate logic to extract information.");
            }

        }


        return tenantForms;
    }


}
