# TP1 — SOAP et WSDL

Service SOAP **Emprunt** de la bibliothèque, construit de deux façons avec JAX-WS (Metro 4.0.2) :

| Projet | Approche | Source de vérité |
|--------|----------|------------------|
| [`emprunt-codefirst/`](emprunt-codefirst/) | Code-first | Les classes Java annotées ; le WSDL est généré au démarrage |
| [`emprunt-contractfirst/`](emprunt-contractfirst/) | Contract-first | [`EmpruntService.wsdl`](emprunt-contractfirst/src/main/resources/wsdl/EmpruntService.wsdl) ; `wsimport` génère l'interface |

Les réponses aux questions de chaque étape sont dans [`reponses.md`](reponses.md).

## Structure

```
TP1-SOAP-WSDL/
├── reponses.md
├── emprunt-codefirst/
│   ├── pom.xml
│   ├── src/main/java/mr/supnum/bib/
│   │   ├── Emprunt.java
│   │   ├── EmpruntService.java          # @WebService : emprunterLivre, rendreLivre
│   │   ├── LivreIndisponibleException.java  # @WebFault
│   │   ├── LivreIndisponibleInfo.java
│   │   └── Serveur.java
│   └── xml/
│       ├── 01-emprunter.xml             # SOAP 1.1, paramètres non qualifiés
│       ├── 02-emprunter-12.xml          # même message en enveloppe SOAP 1.2
│       ├── 03-fault-sender-12.xml       # Fault SOAP 1.2 écrite à la main
│       ├── 04-rendre-erreurs.xml        # message à cinq erreurs (énoncé)
│       └── 04-rendre-corrige.xml        # version corrigée
└── emprunt-contractfirst/
    ├── pom.xml                          # + jaxws-maven-plugin (wsimport)
    ├── src/main/resources/wsdl/EmpruntService.wsdl
    ├── src/main/java/mr/supnum/bib/impl/
    │   ├── EmpruntServiceImpl.java      # implémente EmpruntPortType, @SchemaValidation
    │   └── Serveur.java
    └── xml/
        └── 01-emprunter.xml             # SOAP 1.1, paramètres qualifiés (bib:)
```

## Lancer

Les deux services écoutent sur `http://localhost:8080/bib/EmpruntService` : un seul à la fois.

```bash
cd emprunt-codefirst        # ou emprunt-contractfirst
mvn compile exec:java
# WSDL : http://localhost:8080/bib/EmpruntService?wsdl
```

## Tester

```bash
# Code-first
curl -i -X POST http://localhost:8080/bib/EmpruntService \
  -H 'Content-Type: text/xml; charset=utf-8' -H 'SOAPAction: ""' \
  --data-binary @emprunt-codefirst/xml/01-emprunter.xml

# Contract-first
curl -i -X POST http://localhost:8080/bib/EmpruntService \
  -H 'Content-Type: text/xml; charset=utf-8' \
  -H 'SOAPAction: "http://supnum.mr/bib/emprunterLivre"' \
  --data-binary @emprunt-contractfirst/xml/01-emprunter.xml

# Vérifier la Fault SOAP 1.2 écrite à la main
xmllint --noout emprunt-codefirst/xml/03-fault-sender-12.xml
```

Résultats attendus (détaillés dans `reponses.md`) :

| Cas | Code-first | Contract-first |
|-----|------------|----------------|
| ISBN valide et disponible | 200, `emprunterLivreResponse` | 200, `emprunterLivreResponse` |
| ISBN `978-2-10-074444-4` (déjà sorti) | 500, `Server` + `detail/LivreIndisponible` | 500, `Server` + `detail/LivreIndisponible` |
| ISBN `978-2-10` (mal formé) | 500, `Client` (contrôle Java) | 500, `Client` (validation XSD `cvc-pattern-valid`) |
| Enveloppe SOAP 1.2 | 500, `VersionMismatch` | 500, `VersionMismatch` |
| `bib:jeton` avec `mustUnderstand="1"` | 500, `MustUnderstand` | 500, `MustUnderstand` |
