/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import java.time.OffsetDateTime;

public record Detailplanning(
    String id,
    String plancategorieId,
    String creator,
    OffsetDateTime createdAt,
    String editor,
    OffsetDateTime editedAt,
    Integer jaartal,
    Integer aantalGepland) {}
