/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record Gemeente(
    @Schema(description = "Gemeentecode", example = "GM0687")
    String identificatie,

    @Schema(description = "Naam van de gemeente", example = "Middelburg")
    String naam,

    @Schema(description = "Naam van de provincie", example = "Zeeland")
    String provincie) {}
