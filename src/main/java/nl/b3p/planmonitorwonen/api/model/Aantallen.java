/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Numbers of dwellings of a planregistratie. The maps are keyed by the category value, for example "Eengezins" for
 * woningType. A category left out (or null) is not filled in, a category with an {@link Aantal} of 0 is filled in as
 * zero. For the totals both count as zero. Categories not filled in are left out of the JSON output.
 *
 * @param bruto bruto plancapaciteit, the total number of dwellings added by the plan (stored as plancategorie
 *     "Nieuwbouw")
 * @param sloop number of dwellings demolished
 * @param woningType dwellings by woningtype, should add up to bruto
 * @param wonenEnZorg dwellings by suitability for elderly and care, should add up to bruto
 * @param flexwoningen dwellings by temporary or permanent, should add up to bruto
 * @param betaalbaarheid dwellings by price segment, should add up to bruto
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Aantallen(
    Aantal bruto,
    Aantal sloop,
    Map<String, Aantal> woningType,
    Map<String, Aantal> wonenEnZorg,
    Map<String, Aantal> flexwoningen,
    Map<String, Aantal> betaalbaarheid) {}
