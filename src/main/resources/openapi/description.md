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

## Inloggen

Alle endpoints vereisen een login. Een client logt in zoals de webapplicatie dat doet: met de gebruikersnaam en het
wachtwoord van een Tailormap-account bij `/api/login` van Tailormap, op dezelfde server. Dit geeft een `SESSION` cookie
waarmee de client deze API aanroept. Het account moet een wachtwoord hebben (geen single sign-on) en in een groep zitten
met autorisatie voor de gemeentes waarvoor het planregistraties aanlevert; dat stelt de beheerder van Tailormap in.

1. Haal een XSRF-token op met `OPTIONS /api/login`. Dit zet het `XSRF-TOKEN` cookie.
2. Log in met `POST /api/login` met de formuliervelden `username` en `password` (content type
   `application/x-www-form-urlencoded`) en de header `X-XSRF-TOKEN` met de waarde van het `XSRF-TOKEN` cookie. Het
   antwoord is altijd een redirect (302), volg die niet maar kijk naar de `Location` header: eindigend op
   `/api/unauthorized?error` betekent dat het inloggen mislukt is, anders is het gelukt. Het antwoord zet het
   `SESSION` cookie.
3. Roep deze API aan met het `SESSION` cookie. Een XSRF-token is hier niet nodig.
4. Controleer de login met `GET /api/planmonitor-wonen/{majorVersion}/user`: dit geeft onder andere de gemeentes
   waarvoor de gebruiker planregistraties mag aanleveren.
5. De sessie verloopt na een tijd zonder requests. Een 401 van deze API betekent dat de sessie verlopen is: log
   opnieuw in en herhaal het request.
6. Log uit met `POST /api/logout`. Dit vereist ook de `X-XSRF-TOKEN` header. Inloggen vervangt het XSRF-token, haal
   daarom eerst een nieuw token op zoals bij stap 1.

Voorbeeld met curl, met een cookie jar voor de cookies:

```bash
SERVER=https://planmonitor-wonen.example.nl
API=$SERVER/api/planmonitor-wonen/{majorVersion}
JAR=cookies.txt
PLAN_ID=0b5c1d0e-3f43-4e4a-9a6e-2f1f6b6d2a10
# Met een spatie ervoor komen deze regels niet in de bash history (bij HISTCONTROL=ignorespace of ignoreboth)
 USER=gebruiker
 PASSWORD=wachtwoord

# 1. XSRF-token ophalen
curl -s -c $JAR -X OPTIONS -o /dev/null $SERVER/api/login
XSRF=$(awk '$6 == "XSRF-TOKEN" { print $7 }' $JAR)

# 2. Inloggen, toont de Location van de redirect
curl -s -b $JAR -c $JAR -H "X-XSRF-TOKEN: $XSRF" \
  --data-urlencode "username=$USER" --data-urlencode "password=$PASSWORD" \
  -o /dev/null -w '%{redirect_url}\n' $SERVER/api/login

# 3 en 4. API aanroepen
curl -s -b $JAR $API/user | jq
curl -s -b $JAR -X PUT -H "Content-Type: application/json" --data @plan.json $API/planregistratie/$PLAN_ID | jq
curl -s -b $JAR $API/planregistratie/$PLAN_ID | jq

# 6. Uitloggen
curl -s -b $JAR -c $JAR -X OPTIONS -o /dev/null $SERVER/api/login
XSRF=$(awk '$6 == "XSRF-TOKEN" { print $7 }' $JAR)
curl -s -b $JAR -c $JAR -H "X-XSRF-TOKEN: $XSRF" -X POST $SERVER/api/logout
```

**Swagger UI**: log in dezelfde browser in bij de webapplicatie op deze server, dan stuurt de browser het
`SESSION` cookie mee met de requests van *Try it out*. Een waarde invullen bij *Authorize* werkt niet, een browser
laat een webpagina geen cookies meesturen.

## Aanleveren door een client

De API werkt per planregistratie. Een client kan een plan aanleveren zodra het in de eigen software wordt aangemaakt,
gewijzigd of verwijderd, of alle plannen in één keer synchroniseren, bijvoorbeeld op de vaste aanlevermomenten. De API
kent geen aanlevermomenten of status van een aanlevering, dat gebeurt buiten deze API om.

Alle plannen synchroniseren gaat zo:

1. Log in met het account van de gemeente, zie *Inloggen*.
2. Haal de bestaande planregistraties op met `GET /planregistraties`. Dit geeft alleen de planregistraties van de
   gemeentes waarvoor het account autorisatie heeft, maar ook plannen die via de webapplicatie zijn ingevoerd.
3. Verwijder met `DELETE /planregistratie/{id}` de planregistraties die de client eerder heeft aangeleverd maar die
   niet meer in de eigen software staan. Een 404 betekent dat de planregistratie al verwijderd is.
4. Lever elk nieuw of gewijzigd plan aan met `PUT /planregistratie/{id}`, of altijd alle plannen. Er is geen `POST`:
   de client kiest zelf het id van een nieuwe planregistratie. Het antwoord is 201 voor een nieuwe en 200 voor een
   vervangen planregistratie.

Een fout bij één plan stopt de andere plannen niet. Verzamel de fouten per plan (bij een 400 of 409 met de fouten per
veld in `errors`) en toon ze aan de gebruiker, net als technische fouten zoals een ongeldig account of
netwerkproblemen.

**Id van een planregistratie**: gebruik een UUID versie 5, berekend uit het eigen id van het plan en de gemeentecode
met een vaste namespace-UUID die de client eenmalig kiest. Dan hoeft de client geen koppeling tussen de eigen ids en de
UUIDs op te slaan, en hoeft niet op plannaam gezocht te worden, die kan veranderen. Bijvoorbeeld in Python:

```python
import uuid

NAMESPACE = uuid.UUID("...")  # eenmalig gekozen met uuid.uuid4(), daarna nooit meer wijzigen
plan_id = uuid.uuid5(NAMESPACE, "GM0687:" + eigen_plan_id)
```

Bij stap 3 zijn de planregistraties die de client eerder heeft aangeleverd die met `creator` gelijk aan het account
van de client. Een id dat niet voorkomt bij de ids berekend uit de eigen plannen hoort bij een plan dat verwijderd is.

**Een PUT vervangt de hele planregistratie**: velden die de client niet meestuurt worden leeggemaakt, ook als die in de
webapplicatie zijn ingevuld. Wijzig een plan van de client daarom niet in de webapplicatie, of haal het eerst op met
`GET /planregistratie/{id}` en neem de velden die de client niet zelf bijhoudt over.

## Foutmeldingen

Fouten worden teruggegeven als *problem details* volgens [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) met
content type `application/problem+json`, met in ieder geval `status` en `title` en meestal een `detail` met de reden.

Bij een ongeldige planregistratie (400) of een plannaam die al in gebruik is (409) staan in `errors` alle fouten per
veld: `field` is het pad van het veld, zoals `plantype`, `knelpuntenMeerkeuze[0]` of
`aantallen.woningType.Eengezins.gepland`, en `message` een Nederlandstalige melding die aan de gebruiker getoond kan
worden. Andere fouten, zoals ongeldige JSON, een veld met het verkeerde type of een ongeldig UUID, hebben geen `errors`
en een Engelstalige `detail`.
