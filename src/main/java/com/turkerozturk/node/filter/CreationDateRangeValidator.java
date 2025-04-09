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


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CreationDateRangeValidator implements ConstraintValidator<ValidCreationDateRange, FormSearch> {
    @Override
    public void initialize(ValidCreationDateRange constraintAnnotation) {
    }

    @Override
    public boolean isValid(FormSearch formSearch, ConstraintValidatorContext context) {

        if (formSearch.getTsCreationEndFilter().compareTo(formSearch.getTsCreationStartFilter()) < 0) {
            context.buildConstraintViolationWithTemplate("Creation end date cannot be earlier than creation start date")
                    .addPropertyNode("tsCreationEndFilter")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
