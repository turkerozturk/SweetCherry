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
package com.turkerozturk.global;

import com.turkerozturk.sunandmoon.MoonTime4j;
import com.turkerozturk.helpers.CommonsSunCalc;
import com.turkerozturk.helpers.SolarSystem;
import com.turkerozturk.multipledatabases.MultitenantConfiguration;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.multipledatabases.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CommonsSunCalc commonsSunCalc;

    @Autowired
    private MoonTime4j moonTime4j;

    @Autowired
    private TenantService tenantService;

    @ModelAttribute("astronomyWidget")
    public SolarSystem getAstronomyWidget() {
        // Astronomy widget verisi burada elde edilir veya hesaplanir

        commonsSunCalc.getAstronomy();
        return commonsSunCalc.getSolarSystem();
    }


    @ModelAttribute("moonTime4j")
    public MoonTime4j getMoonTime4jWidget() {
        // Astronomy widget verisi burada elde edilir veya hesaplanir
        return moonTime4j;
    }


    @ModelAttribute("tenantNames")
    public List<String> getTenantNames() {
        // Her bir tenant name, bir veritabanindan digerine gecmek icin belirttigimiz isimler.

        List<String> tenantNames = new ArrayList<>();
        Map<String, DataSource> tenants = tenantService.getAllTenants();
        if(tenants != null && !tenants.isEmpty()) {
            for (Map.Entry<String, DataSource> entry : tenants.entrySet()) {

                tenantNames.add(entry.getKey());
            }
            return tenantNames;

        } else {
            return null;
        }
    }

    @ModelAttribute("currentTenantName")
    public String getCurrentTenantName() {
        return TenantContext.getCurrentTenant();
    }

    @ModelAttribute("ALL_TENANTS_FOLDER")
    public String getAllTenantsFolderName() {
        return MultitenantConfiguration.PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES;
    }

}
