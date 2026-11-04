/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = """
Aantallen woningen van een planregistratie. Een categorie weglaten of `null` betekent niet ingevuld, een categorie
met aantal 0 is ingevuld als nul. Voor de totalen tellen beide als nul. Niet ingevulde categorieën worden in een
response weggelaten.

Regels:
- aantallen zijn niet negatief en `gerealiseerd` is niet meer dan `gepland`
- behalve bij sloop is het totaal van `perJaar` gelijk aan `gepland` min `gerealiseerd`
- voor `woningType`, `wonenEnZorg`, `flexwoningen` en `betaalbaarheid` is het totaal `gepland` van de categorieën
gelijk aan `gepland` van `bruto`, en het totaal per jaar gelijk aan `bruto` in dat jaar. Als `bruto` meer dan 0 is
moeten deze groepen dus allemaal ingevuld worden""")
public record Aantallen(
    @Schema(
        description = "Bruto plancapaciteit: het totaal aantal woningen dat binnen het plan gerealiseerd wordt,"
            + " door nieuwbouw, transformatie of splitsing")
    Aantal bruto,

    @Schema(description = "Aantal woningen dat gesloopt wordt")
    Aantal sloop,

    @Schema(description = "Woningen per woningtype") Map<String, Aantal> woningType,

    @Schema(description = "Woningen per geschiktheid voor ouderen en zorg")
    Map<String, Aantal> wonenEnZorg,

    @Schema(description = "Woningen per tijdelijk (flexwoningen) of permanent")
    Map<String, Aantal> flexwoningen,

    @Schema(description = "Woningen per prijssegment") Map<String, Aantal> betaalbaarheid) {}
