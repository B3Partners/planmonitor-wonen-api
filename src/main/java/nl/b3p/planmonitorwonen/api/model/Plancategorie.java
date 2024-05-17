/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import java.time.OffsetDateTime;

public record Plancategorie(
    String id,
    String planregistratieId,
    String creator,
    OffsetDateTime createdAt,
    String editor,
    OffsetDateTime editedAt,
    String nieuwbouw,
    String woningType,
    String wonenEnZorg,
    String flexwoningen,
    String betaalbaarheid,
    String sloop,
    Integer totaalGepland,
    Integer totaalGerealiseerd) {}
