API van Planmonitor Wonen voor het opvragen, aanleveren en verwijderen van planregistraties (woningbouwplannen), onder
andere voor het per plan aanleveren van de basisset 2.0 door software van gemeentes.

## Versies

De major-versie van de API staat in het pad: `/api/planmonitor-wonen/{majorVersion}/...`. Een nieuwe major-versie betekent
wijzigingen die niet backwards-compatible zijn, zoals het verwijderen of hernoemen van een endpoint of veld, het verplicht
maken van een veld of een andere betekenis van een veld. Deze server biedt alleen versie `{majorVersion}` aan, een andere
versie in het pad geeft een 400-fout.

Binnen een major-versie wordt de minor-versie verhoogd voor backwards-compatible toevoegingen, zoals een nieuw endpoint,
een nieuw optioneel veld in een request, een nieuw veld in een response of een nieuwe waarde in een keuzelijst. Een client
moet daarom onbekende velden in een response negeren. De volledige versie (major.minor, nu `{version}`) staat in de
`API-Version` response header en in deze documentatie.

## Foutmeldingen

Fouten worden teruggegeven als *problem details* volgens [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) met
content type `application/problem+json`, met in ieder geval `status` en `title` en meestal een `detail` met de reden.

Bij een ongeldige planregistratie (400) of een plannaam die al in gebruik is (409) staan in `errors` alle fouten per
veld: `field` is het pad van het veld, zoals `plantype`, `knelpuntenMeerkeuze[0]` of
`aantallen.woningType.Eengezins.gepland`, en `message` een Nederlandstalige melding die aan de gebruiker getoond kan
worden. Andere fouten, zoals ongeldige JSON, een veld met het verkeerde type of een ongeldig UUID, hebben geen `errors`
en een Engelstalige `detail`.
