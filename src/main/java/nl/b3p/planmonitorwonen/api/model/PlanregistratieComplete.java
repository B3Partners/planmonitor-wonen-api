/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

public record PlanregistratieComplete(
    Planregistratie planregistratie,
    Plancategorie[] plancategorieen,
    Detailplanning[] detailplanningen) {}
