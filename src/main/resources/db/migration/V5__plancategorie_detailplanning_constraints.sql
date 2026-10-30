-- Copyright (C) 2026 Provincie Zeeland
--
-- SPDX-License-Identifier: MIT

-- Sleutelproject is specific for the provincie and not part of the basisset 2.0, so not required for plans delivered
-- through the API
alter table planregistratie alter column sleutelproject drop not null;

-- A plancategorie is one value of one of the categorie groups, and occurs at most once per planregistratie
alter table plancategorie
    add constraint plancategorie_een_groep
        check (num_nonnulls(nieuwbouw, woning_type, wonen_en_zorg, flexwoningen, betaalbaarheid, sloop) = 1),
    add constraint plancategorie_uniek
        unique nulls not distinct (planregistratie_id, nieuwbouw, woning_type, wonen_en_zorg, flexwoningen, betaalbaarheid, sloop);

-- At most one detailplanning per year per plancategorie
alter table detailplanning
    alter column jaartal set not null,
    add constraint detailplanning_jaartal_uniek unique (plancategorie_id, jaartal);
