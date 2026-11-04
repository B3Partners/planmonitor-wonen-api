# planmonitor-wonen-api
Rest API voor Planmonitor Wonen. Planmonitor Wonen is een webapplicatie die inzicht geeft in de voortgang van de woningbouwopgave in Nederland. De applicatie is ontwikkeld in opdracht van het Ministerie van Binnenlandse Zaken en Koninkrijksrelaties en is bedoeld voor beleidsmakers, bestuurders, onderzoekers en andere geïnteresseerden die inzicht willen in de voortgang van de woningbouwopgave in Nederland.

## API documentatie

De API is gedocumenteerd met OpenAPI. Open in een browser `/api/planmonitor-wonen/` om de API te bekijken en uit te
proberen met Swagger UI. De OpenAPI-specificatie zelf is te downloaden via `/api/planmonitor-wonen/api-docs`.

De API-documentatie en Swagger UI staan standaard aan. Zet in productie de omgevingsvariabele
`PLANMONITOR_WONEN_API_OPENAPI_ENABLED=false` (of `PMW_OPENAPI_ENABLED=false` in de Docker Compose-configuratie van
tailormap-planmonitor-wonen) om deze uit te schakelen.

## Health check

Met de URL `/api/planmonitor-wonen/actuator/health` (geen authenticatie vereist) kan de status van de applicatie
gecontroleerd worden: deze moet HTTP status 200 retourneren met body `{"status":"UP"}`. Direct na het starten
retourneert deze tijdelijk HTTP status 503 totdat de gegevens voor het automatisch invullen uit de WFS-services zijn
geladen.

Versie-informatie staat op `/api/planmonitor-wonen/actuator/info`. Hiervoor moet de gebruiker ingelogd zijn in
Tailormap met groep `admin` of `actuator`.
