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
package com.turkerozturk.node.filter;


import com.turkerozturk.helpers.NodeIcon;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

import java.util.LinkedHashSet;

public class NodeIconValidator implements ConstraintValidator<ValidNodeIcons, LinkedHashSet<NodeIcon>> {
    @Override
    public void initialize(ValidNodeIcons constraintAnnotation) {
    }

    @Override
    public boolean isValid(LinkedHashSet<NodeIcon> nodeIcons, ConstraintValidatorContext context) {

        if(nodeIcons != null) {
            for (NodeIcon nodeIcon : nodeIcons) {
                if (!EnumUtils.isValidEnum(NodeIcon.class, nodeIcon.name())) {
                    return false;
                }
            }
        }

        return true;
    }
}
