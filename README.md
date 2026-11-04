# planmonitor-wonen-api
Rest API voor Planmonitor Wonen. Planmonitor Wonen is een webapplicatie die inzicht geeft in de voortgang van de
woningbouwopgave. Gemeenten registreren hun woningbouwplannen in de webapplicatie, of leveren ze via deze API aan vanuit
hun eigen software.

De applicatie is ontwikkeld door B3Partners B.V. voor Provincie Zeeland en bevat een paar onderdelen die specifiek zijn
voor Zeeland:

- de provinciespecifieke velden van een planregistratie, zie *Verschillen met de basisset 2.0*;
- de frontend ([tailormap-planmonitor-wonen](https://github.com/B3Partners/tailormap-planmonitor-wonen)) haalt de
  gemeentes op voor de vaste provincie "Zeeland";
- de WFS-service waaruit de regio en het beoogde woonmilieu worden afgeleid, is die van Provincie Zeeland
  (`planmonitor-wonen-api.wfs.ruimte` in `application.properties`). De provincie- en gemeentegrenzen komen uit de
  landelijke WFS-service van PDOK.

Met een paar kleine aanpassingen kan de applicatie ook door andere provincies worden gebruikt, inclusief deze API voor
het aanleveren van plannen door gemeenten met software die deze API ondersteunt. Neem daarvoor contact op met Provincie
Zeeland.

## API documentatie

De API is gedocumenteerd met OpenAPI. Open in een browser `/api/planmonitor-wonen/` om de API te bekijken en uit te
proberen met Swagger UI. De OpenAPI-specificatie zelf is te downloaden via `/api/planmonitor-wonen/api-docs`.

De API-documentatie en Swagger UI staan standaard aan. Zet in productie de omgevingsvariabele
`PLANMONITOR_WONEN_API_OPENAPI_ENABLED=false` (of `PMW_OPENAPI_ENABLED=false` in de Docker Compose-configuratie van
tailormap-planmonitor-wonen) om deze uit te schakelen.

## Verschillen met de basisset 2.0

Het gegevensmodel van Planmonitor Wonen is opgezet voor de provincie en komt niet helemaal overeen met de
[basisset 2.0](https://vng.nl/sites/default/files/2024-09/lbr_24_036_basisset_2_0_definitief_met_logos.pdf). Hieronder
staat hoe de API daar nu mee omgaat, ter informatie voor de software van gemeenten die plannen aanlevert.

### Provinciespecifieke velden

Via de API zijn alleen de velden verplicht die in de basisset staan of nodig zijn om een plan te herkennen: plannaam,
gemeente, plangrens, plantype en planologische status. De overige velden zijn provinciespecifiek. In de webapplicatie
zijn die wel verplicht, via de API niet:

| Veld | Via de API |
|---|---|
| Provincie | wordt door de server ingevuld, afgeleid uit de gemeente |
| Regio | wordt door de server ingevuld als het ontbreekt, afgeleid uit de ligging van de plangrens |
| Beoogd woonmilieu (ABF13) | wordt door de server ingevuld als het ontbreekt, afgeleid uit de ligging van de plangrens |
| Vertrouwelijkheid | standaard "Openbaar" als het ontbreekt |
| Plaatsnaam | optioneel |
| Opdrachtgever (type) en naam opdrachtgever | optioneel |
| Projectstatus | optioneel |
| Knelpunten | optioneel |
| Aantal studentenwoningen | optioneel |
| Sleutelproject | optioneel |

Als een optioneel veld wordt meegestuurd, controleert de API wel of de waarde geldig is. Een plan dat zonder deze velden
is aangeleverd, kan in de webapplicatie pas worden opgeslagen als de ontbrekende velden zijn ingevuld.

Verder:

- Een plan kan als vertrouwelijk worden gemarkeerd, ongeacht de planologische status.
- Plannen met planologische status "5A. Ambtelijk met locatie" en "5B. Ambtelijk zonder locatie" worden geaccepteerd,
  hoewel deze volgens de basisset niet aan de provincie worden geleverd.
- Een plannaam mag maar één keer voorkomen, over alle gemeenten heen. Een plan met een naam die al bij een andere
  gemeente in gebruik is, wordt geweigerd (409).

### Gegevens uit de basisset die niet kunnen worden opgeslagen

- **Prijssegment en eigendom.** De API kent alleen de indeling in betaalbaarheid (`aantallen.betaalbaarheid`: sociale
  huur, middenhuur, dure huur, betaalbare koop, dure koop en onbekend). Het eigendom (corporatie of markt) en de
  instandhoudingsplicht uit de basisset kunnen niet worden opgeslagen, en het prijssegment uit de basisset is niet één
  op één om te zetten naar deze indeling.
- **Ouderen.** Bij wonen en zorg (`aantallen.wonenEnZorg`) zijn er de categorieën regulier, nultreden, geclusterd,
  zorggeschikt en onbekend. De categorie "overige geschikt voor ouderen" en de twee vragen over 50% of meer
  55-plussers uit de basisset ontbreken.
- **Vervallen.** Er is geen veld om aan te geven dat een plan is vervallen. Een plan dat niet meer in de Planmonitor
  Wonen hoort te staan, kan met `DELETE /planregistratie/{id}` worden verwijderd.
- **Fasering.** De basisset gaat uit van een periode die elk jaar opschuift: de eerste 10 jaar per jaar, daarna twee
  perioden van 5 jaar. De API heeft in `perJaar` alleen afzonderlijke jaren, van 2024 tot en met 2043. Ook de jaren van
  de perioden van 5 jaar worden dus per jaar aangeleverd.

## Draaien en ontwikkelen

### Health check

Met de URL `/api/planmonitor-wonen/actuator/health` (geen authenticatie vereist) kan de status van de applicatie
gecontroleerd worden: deze moet HTTP status 200 retourneren met body `{"status":"UP"}`. Direct na het starten
retourneert deze tijdelijk HTTP status 503 totdat de gegevens voor het automatisch invullen uit de WFS-services zijn
geladen.

Versie-informatie staat op `/api/planmonitor-wonen/actuator/info`. Hiervoor moet de gebruiker ingelogd zijn in
Tailormap met groep `admin` of `actuator`.
