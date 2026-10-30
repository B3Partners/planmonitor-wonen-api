-- Copyright (C) 2026 Provincie Zeeland
--
-- SPDX-License-Identifier: MIT

-- Creator and editor are only kept for the planregistratie, these were never filled in by the frontend
alter table plancategorie
    drop column creator,
    drop column created_at,
    drop column editor,
    drop column edited_at;

alter table detailplanning
    drop column creator,
    drop column created_at,
    drop column editor,
    drop column edited_at;
