-- Copyright (C) 2024 Provincie Zeeland
--
-- SPDX-License-Identifier: MIT

create table gemeente
(
    identificatie varchar primary key,
    naam          varchar unique            not null,
    provincie     varchar                   not null,
    geometry      Geometry(Geometry, 28992) not null
);

create type pmw_vertrouwelijkheid as enum (
    'Gemeente',
    'Openbaar',
    'Provincie');

create type pmw_opdrachtgever as enum (
    'Anders',
    'Gemeente',
    'Meerdere namelijk',
    'Nog onbekend',
    'Particulieren',
    'Projectontwikkelaar',
    'Woningbouwcorporatie');

create type pmw_plantype as enum (
    'Pand Transformatie',
    'Transformatiegebied',
    'Herstructurering',
    'Verdichting',
    'Uitbreiding uitleg',
    'Uitbreiding overig');

create type pmw_project_status as enum (
    'Studie',
    'Start',
    'Initiatief',
    'Definitie',
    'Ontwerp',
    'Voorbereiding',
    'Bouwvoorbereiding',
    'Realisatie tot eerste woning',
    'Realisatie tot laatste woning',
    'Nazorg',
    'Afgerond',
    'Onbekend');

create type pmw_planologisch_status as enum (
    '1A. Onherroepelijk',
    '1B. Onherroepelijk, uitwerkingsplicht',
    '1C. Onherroepelijk, wijzigingsbevoegdheid',
    '2A. Vastgesteld',
    '2B. Vastgesteld, uitwerkingsplicht',
    '2C. Vastgesteld, wijzigingsbevoegdheid',
    '3. In voorbereiding',
    '4A. Visie',
    '4B. Optie');

create type pmw_knelpunten_meerkeuze as enum (
    'Ambtelijke capaciteit',
    'Anders',
    'Bereikbaarheid',
    'Geluidshinder',
    'Maatschappelijk draagvlak',
    'Onrendabele top',
    'Stikstof',
    'Vervuilde grond',
    'Flora/fauna',
    'Netcongestie');

create type pmw_eigendom as enum (
    'Koopwoning',
    'Huurwoning particuliere verhuurder',
    'Huurwoning woningcorporatie',
    'Onbekend');

create type pmw_knelpunten_plantype as enum (
    'Herstructurering',
    'Onbekend',
    'Transformatie gebied',
    'Transformatie gebouw',
    'Uitbreiding overig',
    'Uitbreiding uitleg',
    'Verdichting');

create type pmw_woonmilieu_abf5 as enum (
    'Buitencentrum',
    'Centrum-stedelijk',
    'Dorps',
    'Groen-stedelijk',
    'Landelijk');

create type pmw_woonmilieu_abf13 as enum (
    'Centrum-dorps',
    'Centrum-kleinstedelijk',
    'Centrum-stedelijk',
    'Centrum-stedelijk-plus',
    'Dorps',
    'Groen-kleinstedelijk',
    'Groen-stedelijk',
    'Kleinstedelijk',
    'Landelijk bereikbaar',
    'Landelijk perifeer',
    'Stedelijk naoorlogs compact',
    'Stedelijk naoorlogs grondgebonden',
    'Stedelijk vooroorlogs');

create table planregistratie
(
    id                       uuid                     not null primary key default gen_random_uuid(),
    geometrie                Geometry(Polygon, 28992) not null,
    creator                  varchar                  not null,
    created_at               timestamp with time zone not null             default now(),
    editor                   varchar,
    edited_at                timestamp with time zone,
    plan_naam                varchar                  not null unique,
    provincie                varchar,
    gemeente                 varchar references gemeente (naam),
    regio                    varchar,
    plaatsnaam               varchar,
    vertrouwelijkheid        pmw_vertrouwelijkheid,
    opdrachtgever_type       pmw_opdrachtgever,
    opdrachtgever_naam       varchar,
    opmerkingen              text,
    plantype                 pmw_plantype,
    bestemmingsplan          varchar,
    status_project           pmw_project_status,
    status_planologisch      pmw_planologisch_status,
    knelpunten_meerkeuze     pmw_knelpunten_meerkeuze,
    beoogd_woonmilieu_abf13  pmw_woonmilieu_abf13,
    aantal_studentenwoningen integer,
    sleutelproject           boolean not null
);

create type pmw_nieuwbouw as enum (
    'Nieuwbouw');

create type pmw_woning_type as enum (
    'Eengezins',
    'Meergezins',
    'Onbekend');

create type pmw_wonen_en_zorg as enum (
    'Geclusterd',
    'Nultreden',
    'Onbekend',
    'Regulier',
    'Zorggeschikt');

create type pmw_flexwoningen as enum (
    'Flexwoningen',
    'Regulier permanent');

create type pmw_betaalbaarheid as enum (
    'Sociale huur',
    'Huur middenhuur',
    'Huur dure huur',
    'Huur onbekend',
    'Koop betaalbare koop',
    'Koop dure koop',
    'Koop onbekend',
    'Onbekend koop of huur');

create type pmw_sloop as enum (
    'Sloop');

create table plancategorie
(
    id                  uuid not null primary key default gen_random_uuid(),
    planregistratie_id  uuid not null references planregistratie (id) on delete cascade,
    creator             varchar,
    created_at          timestamp with time zone  default now(),
    editor              varchar,
    edited_at           timestamp with time zone,
    nieuwbouw           pmw_nieuwbouw,
    woning_type         pmw_woning_type,
    wonen_en_zorg       pmw_wonen_en_zorg,
    flexwoningen        pmw_flexwoningen,
    betaalbaarheid      pmw_betaalbaarheid,
    sloop               pmw_sloop,
    totaal_gepland      integer,
    totaal_gerealiseerd integer
);

create table detailplanning
(
    id               uuid not null primary key default gen_random_uuid(),
    plancategorie_id uuid not null references plancategorie (id) on delete cascade,
    creator          varchar,
    created_at       timestamp with time zone  default now(),
    editor           varchar,
    edited_at        timestamp with time zone,
    jaartal          integer,
    aantal_gepland   integer
);