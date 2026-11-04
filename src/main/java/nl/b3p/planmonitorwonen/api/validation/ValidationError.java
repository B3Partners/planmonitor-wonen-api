/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A validation error for a field of the request body.
 *
 * @param field the field, for example "plantype" or "aantallen.woningType.Eengezins.gepland"
 * @param message a message for the user, in Dutch
 */
@Schema(description = "Fout in een veld van de planregistratie")
public record ValidationError(
    @Schema(
        description = "Pad van het veld in de request body",
        example = "aantallen.woningType.Eengezins.gepland",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String field,

    @Schema(
        description = "Nederlandstalige melding voor de gebruiker",
        example = "Gepland mag niet negatief zijn",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String message) {}
