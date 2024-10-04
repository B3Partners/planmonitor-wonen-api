/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import java.util.List;

public record PlanregistratieComplete(
    Planregistratie planregistratie,
    List<Plancategorie> plancategorieen,
    List<Detailplanning> detailplanningen) {}
