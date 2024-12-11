# IMX Fieldlab: proactieve dienstverlening 

- Demo API: https://imx.apps.digilab.network/fieldlab-pdv

## Configuratie

- [Doelmodel](./config/pdv.yaml)
- [Model mapping](./config/pdv.mapping.yaml)
- Bronnen:
  - BRI ([Model](./config/bri.yaml) / [API](https://gitlab.com/digilab.overheid.nl/ecosystem/fdsdemo/-/blob/main/belastingdienst/api/openapi.yaml))
  - BRP ([Model](./config/brp.yaml) / [API](https://brp-api.github.io/Haal-Centraal-BRP-bevragen/v2/redoc))
  - UWV ([Model](./config/uwv.yaml) / [API](https://gitlab.com/digilab.overheid.nl/ecosystem/fdsdemo/-/blob/main/uwv/api/openapi.yaml))

## Voorbeeld queries

```graphql
query Persoon {
  persoon(bsn: "999990639") {
    bsn
    isIngezetene
    leeftijd
    vermogen
    inkomenMaandelijks
    inkomenJaarlijks
  }
}

query BeschikkingZorgtoeslag1 {
  beschikkingZorgtoeslag(bsn: "999993653") {
    gerechtigd
    bedrag
    toelichting
  }
}

query BeschikkingZorgtoeslag2 {
  beschikkingZorgtoeslag(bsn: "999990639") {
    gerechtigd
    bedrag
    toelichting
  }
}
```