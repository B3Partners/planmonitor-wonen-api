/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import nl.b3p.planmonitorwonen.api.validation.ValidationError;

/**
 * Only used to document the format of error responses in the OpenAPI documentation. The responses are created by Spring
 * as a {@link org.springframework.http.ProblemDetail}, which has its extra properties in a map not suitable for
 * documenting.
 */
@Schema(name = "ProblemDetail", description = "Foutmelding volgens RFC 9457 (content type application/problem+json)")
public record ProblemDetailSchema(
    @Schema(description = "URI van het type fout, meestal `about:blank`", example = "about:blank")
    String type,

    @Schema(description = "Korte omschrijving van het type fout", example = "Ongeldige planregistratie")
    String title,

    @Schema(description = "HTTP-statuscode", example = "400")
    int status,

    @Schema(description = "Omschrijving van de fout", example = "De planregistratie bevat 1 fout")
    String detail,

    @Schema(
        description = "Pad van het request",
        example = "/api/planmonitor-wonen/v2/planregistratie/0b5c1d0e-3f43-4e4a-9a6e-2f1f6b6d2a10")
    String instance,

    @Schema(
        description = "Alle fouten per veld, alleen bij een ongeldige planregistratie (400) of een plannaam"
            + " die al in gebruik is (409)",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    List<ValidationError> errors) {}
