# planmonitor-wonen-api
Rest API voor Planmonitor Wonen. Planmonitor Wonen is een webapplicatie die inzicht geeft in de voortgang van de woningbouwopgave in Nederland. De applicatie is ontwikkeld in opdracht van het Ministerie van Binnenlandse Zaken en Koninkrijksrelaties en is bedoeld voor beleidsmakers, bestuurders, onderzoekers en andere geïnteresseerden die inzicht willen in de voortgang van de woningbouwopgave in Nederland.

## API documentatie

De API is gedocumenteerd met OpenAPI. Open in een browser `/api/planmonitor-wonen/` om de API te bekijken en uit te
proberen met Swagger UI. De OpenAPI-specificatie zelf is te downloaden via `/api/planmonitor-wonen/v3/api-docs`.

De API-documentatie en Swagger UI staan standaard aan. Zet in productie de omgevingsvariabele
`PLANMONITOR_WONEN_API_OPENAPI_ENABLED=false` (of `PMW_OPENAPI_ENABLED=false` in de Docker Compose-configuratie van
tailormap-planmonitor-wonen) om deze uit te schakelen.
