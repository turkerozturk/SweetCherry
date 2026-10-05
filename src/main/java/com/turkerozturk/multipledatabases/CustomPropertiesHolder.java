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

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomPropertiesHolder {
    private final Map<String, Map<String, String>> customPropertiesMap = new java.util.concurrent.ConcurrentHashMap<>();

    public void addCustomProperties(String tenantName, Map<String, String> properties) {
        customPropertiesMap.put(tenantName, properties);
    }

    /** Treats removal without a selected tenant as a no-op. */
    public void removeCustomProperties(String tenantName) {
        if (tenantName != null) customPropertiesMap.remove(tenantName);
    }

    /** Returns no settings when a tenant is absent, so callers can apply their existing defaults. */
    public Map<String, String> getCustomProperties(String tenantName) {
        return tenantName == null ? null : customPropertiesMap.get(tenantName);
    }
}

