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
    'Vervuilde grond');

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
    gemeente                 varchar,
    regio                    varchar,
    plaatsnaam               varchar,
    vertrouwelijkheid        pmw_vertrouwelijkheid,
    opdrachtgever_type       pmw_opdrachtgever,
    opdrachtgever_naam       varchar,
    jaar_start_project       integer,
    oplevering_eerste        integer,
    oplevering_laatste       integer,
    opmerkingen              text,
    plantype                 pmw_plantype,
    bestemmingsplan          varchar,
    status_project           pmw_project_status,
    status_planologisch      pmw_planologisch_status,
    knelpunten_meerkeuze     pmw_knelpunten_meerkeuze,
    regionale_planlijst      pmw_eigendom,
    toelichting_knelpunten   pmw_knelpunten_plantype,
    flexwoningen             integer,
    levensloopbestendig_ja   integer,
    levensloopbestendig_nee  integer,
    beoogd_woonmilieu_abf5   pmw_woonmilieu_abf5,
    beoogd_woonmilieu_abf13  pmw_woonmilieu_abf13,
    aantal_studentenwoningen integer,
    toelichting_kwalitatief  varchar
);
