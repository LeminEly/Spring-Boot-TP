# TP1 — SOAP et WSDL : réponses

**Étudiant :** Mohamed Lemine Ely — matricule 24209
**Module :** Architecture SOA & Web Services — SupNum 2026-2027

Environnement : JDK 17 (Temurin 17.0.14), Maven 3.9.12, Metro JAX-WS RI 4.0.2, curl 8.22.
Toutes les réponses ci-dessous s'appuient sur les messages réellement envoyés et reçus ;
les extraits cités sont recopiés des réponses du serveur.

---

## Partie A — Code-first (`emprunt-codefirst`)

### Étape 1 — Créer et lancer le service

**1. Annotations.**
`@WebService` (sur `EmpruntService`) transforme la classe en service web : JAX-WS en dérive le
`portType`, le `binding` et le `service` du WSDL. `@WebFault` (sur `LivreIndisponibleException`)
déclare l'exception comme Fault du contrat : elle devient un `wsdl:message` de fault
(`LivreIndisponibleException`) dont la partie est l'élément `tns:LivreIndisponible`.

**2. Constructeur vide.**
JAXB désérialise le XML par réflexion : il instancie d'abord l'objet avec le constructeur sans
argument, puis affecte chaque champ à partir des éléments lus. Sans ce constructeur, JAXB ne peut
pas créer l'objet : il signale la classe comme invalide (*does not have a no-arg default
constructor*) lors de la construction de son modèle.

### Étape 2 — Lire le WSDL généré

**1. URL et opérations.**
- Service : `http://localhost:8080/bib/EmpruntService` (élément `soap:address` du port `EmpruntPort`).
- `emprunterLivre` : entrée `tns:emprunterLivre`, sortie `tns:emprunterLivreResponse`,
  fault `tns:LivreIndisponibleException`.
- `rendreLivre` : entrée `tns:rendreLivre`, sortie `tns:rendreLivreResponse`, aucune fault déclarée.

**2. Style et version.**
`<soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>` et
`<soap:body use="literal"/>` : **document/literal** (wrapped). Le préfixe `soap` pointe vers
`http://schemas.xmlsoap.org/wsdl/soap/`, l'extension WSDL de **SOAP 1.1**
(SOAP 1.2 utiliserait `http://schemas.xmlsoap.org/wsdl/soap12/`).

**3. Types générés.**
Dans `?xsd=1`, `isbn` et `dateRetour` sont tous deux `xs:string` (avec `minOccurs="0"`).
La règle de format de l'ISBN **n'apparaît pas** : elle vit dans une `Pattern` Java, que JAX-WS ne
peut pas traduire en facette XSD. Le contrat ne dit donc rien de ce que le service exige vraiment.

**4. Préfixe des paramètres.**
Non. Le schéma généré ne déclare pas `elementFormDefault`, qui vaut alors `unqualified` : seul
l'élément global `bib:emprunterLivre` est dans l'espace de noms, ses enfants `idAdherent` et
`isbn` sont écrits **sans préfixe**. C'est la forme générée par SoapUI pour ce WSDL.

**5. HTTP, XML, SOAP et WSDL dans l'échange brut.** (capture `curl -v`, équivalente à l'onglet *Raw*)
```
POST /bib/EmpruntService HTTP/1.1          <- HTTP : méthode, chemin, en-têtes
Content-Type: text/xml;charset=UTF-8
SOAPAction: ""

<soap:Envelope ...><soap:Header>...</soap:Header><soap:Body>...</soap:Body></soap:Envelope>
                                           <- XML (syntaxe) organisé selon SOAP (Envelope/Header/Body)
HTTP/1.1 200 OK
Content-type: text/xml; charset=utf-8
<S:Envelope ...><S:Body><ns2:rendreLivreResponse>...
```
- **HTTP** transporte le message : ligne de requête, en-têtes, code de statut.
- **XML** fournit la syntaxe et les espaces de noms.
- **SOAP** fixe la structure du message (`Envelope`, `Header` optionnel, `Body`, `Fault`).
- **WSDL** n'apparaît pas dans l'échange : c'est le contrat lu *avant*, qui dit à quelle adresse
  envoyer, quel élément mettre dans le `Body`, quels types et quelles Faults attendre.

### Étape 3 — Envoyer des messages SOAP

Fichiers : `xml/01-emprunter.xml`, `xml/02-emprunter-12.xml`, `xml/04-rendre-erreurs.xml`,
`xml/04-rendre-corrige.xml`.

Envoi de `01-emprunter.xml` : `HTTP/1.1 200 OK` et
`<emprunt><numero>E-2026-0812</numero><dateRetour>2026-12-08</dateRetour></emprunt>`.

**1. Pourquoi `POST` ?**
Le message SOAP voyage dans le **corps** de la requête, et `GET` n'en a pas. SOAP n'utilise pas la
sémantique des méthodes HTTP : une seule URL reçoit tous les appels, l'opération est désignée par
l'élément du `Body` (et éventuellement `SOAPAction`). HTTP n'est qu'un transport ; même une lecture
passe donc en `POST`.

**2. Le bloc `jeton` a-t-il bloqué le traitement ?**
Non (HTTP 200). Aucun handler ne traite `bib:jeton` ; un bloc d'en-tête non compris est ignoré
**sauf** s'il porte `mustUnderstand="1"` (voir étape 4).

**3. Message SOAP 1.2.**
Avec `Content-Type: text/xml`, le service répond HTTP 500 :
```
<faultcode>S:VersionMismatch</faultcode>
<faultstring>Couldn't create SOAP message. Expecting Envelope in namespace
http://schemas.xmlsoap.org/soap/envelope/, but got http://www.w3.org/2003/05/soap-envelope</faultstring>
```
Le binding lu à l'étape 2 est SOAP 1.1 : l'endpoint n'accepte que l'enveloppe
`http://schemas.xmlsoap.org/soap/envelope/` et répond par la Fault prévue par la norme pour une
version inconnue. Avec le `Content-Type` propre à SOAP 1.2 (`application/soap+xml`), la requête est
rejetée plus tôt : **HTTP 415 Unsupported Media Type**, sans corps.

**4. Les cinq erreurs.** Corrigées une par une ; chaque ligne donne la réponse obtenue avant la
correction suivante.

| # | Erreur | Correction | Réaction du serveur |
|---|--------|------------|---------------------|
| 1 | `soap:envelope` en minuscule (XML est sensible à la casse) | `soap:Envelope` | `S:Client` — *unexpected XML tag. expected: {…}Envelope but found: {…}envelope* |
| 2 | Espace de noms `http://supnum.mr/bibliotheque` | `http://supnum.mr/bib` | `S:Client` — *Cannot find dispatch method for {http://supnum.mr/bibliotheque}rendreLivre* |
| 3 | `<bib:rendreLivre>` fermé par `</rendreLivre>` : XML mal formé | `</bib:rendreLivre>` | `S:Server` — *Unexpected close tag &lt;/rendreLivre&gt;; expected &lt;/bib:rendreLivre&gt;* |
| 4 | `Header` placé **après** `Body` (SOAP 1.1 impose Header puis Body) | Déplacer le `Header` avant le `Body` | **Aucune erreur** : HTTP 200 |
| 5 | Paramètre `bib:numero` au lieu de `numeroEmprunt` non qualifié | `<numeroEmprunt>E-2026-0812</numeroEmprunt>` | **Aucune erreur** : HTTP 200, `penalite` 0 |

Les erreurs 4 et 5 passent parce que le service **ne valide pas** les messages contre un schéma :
- 4 : le runtime lit les en-têtes et le corps quel que soit leur ordre ; il ne vérifie pas la
  structure imposée par la spécification.
- 5 : JAXB ignore l'élément inconnu `bib:numero` ; le paramètre `numeroEmprunt` reste `null` et la
  méthode répond quand même. L'erreur est **silencieuse**, et c'est la plus dangereuse.

L'erreur 3 est même classée `Server`, alors que le message mal formé est bien une faute du client.

Une fois le contrôle `E-2026-` de l'étape 4 ajouté, l'erreur 5 devient visible :
`S:Client — Numéro d'emprunt inconnu : null`. Le tableau décrit le service tel qu'il était à
l'étape 3 ; les deux cas ont été vérifiés.

### Étape 4 — Provoquer et construire des Faults

**1. Cas 1 et 2.**

| Cas | ISBN | faultcode | faultstring | HTTP |
|-----|------|-----------|-------------|------|
| 1 | `978-2-10` | `S:Client` | `ISBN mal formé : 978-2-10` | 500 |
| 2 | `978-2-10-074444-4` | `S:Server` | `Livre indisponible` | 500 |

SOAP 1.1 sur HTTP renvoie toujours **500** pour une Fault, que la faute vienne du client ou du serveur.

**2. Contenu de `<detail>` (cas 2).**
```xml
<detail>
  <ns2:LivreIndisponible xmlns:ns2="http://supnum.mr/bib">
    <isbn>978-2-10-074444-4</isbn><retourPrevu>2026-12-02</retourPrevu>
  </ns2:LivreIndisponible>
</detail>
```
La structure est décrite dans le WSDL : le `wsdl:message LivreIndisponibleException` a pour partie
l'élément `tns:LivreIndisponible`, défini dans le XSD (`?xsd=1`) avec le type `livreIndisponibleInfo`.
Le `portType` le rattache à `emprunterLivre` par `<fault name="LivreIndisponibleException">`.

**3. Code choisi pour le livre indisponible.**
JAX-WS a choisi **`Server`** : c'est le code par défaut de toute exception applicative déclarée par
`@WebFault`. Pourtant, le serveur fonctionne normalement : c'est la demande qui ne peut pas être
satisfaite. Renvoyer la même requête donnera la même erreur tant que le livre n'est pas rendu. Il
s'agit donc d'une erreur **côté client** (au sens SOAP : « ne pas renvoyer ce message tel quel »).
J'aurais choisi **`Client`** (`Sender` en SOAP 1.2), en gardant le `detail` structuré qui indique
la date de retour prévue. Un `Server` incite à tort le client à réessayer.

**4. `mustUnderstand`.**
- `mustUnderstand="1"` : HTTP 500,
  `S:MustUnderstand — MustUnderstand headers:[{http://supnum.mr/bib}jeton] are not understood`.
  Le destinataire **doit** traiter ce bloc ou refuser tout le message ; aucun handler ne déclare
  comprendre `jeton`, donc le traitement est refusé avant d'appeler la méthode.
- `mustUnderstand="0"` : HTTP 200 et emprunt créé. C'est la valeur par défaut, et le bloc est
  facultatif : il est ignoré s'il n'est pas compris.

**Rendre avec un numéro invalide** (contrôle ajouté) : `X-1` donne
`S:Client — Numéro d'emprunt inconnu : X-1`, HTTP 500.

**5. Différences entre ma Fault 1.2 (`xml/03-fault-sender-12.xml`, validée par `xmllint --noout`) et
les Faults 1.1 reçues.**

| SOAP 1.1 (reçue) | SOAP 1.2 (écrite) |
|------------------|-------------------|
| Enveloppe `http://schemas.xmlsoap.org/soap/envelope/` | `http://www.w3.org/2003/05/soap-envelope` |
| `faultcode` (texte, non qualifié) | `soap:Code/soap:Value`, extensible par `soap:Subcode` |
| Codes `Client` / `Server` | Codes `Sender` / `Receiver` |
| `faultstring`, une seule chaîne sans langue | `soap:Reason/soap:Text xml:lang="fr"`, plusieurs langues possibles |
| `detail` | `soap:Detail` |
| Éléments enfants sans espace de noms | Éléments enfants dans l'espace de noms SOAP 1.2 |
| HTTP 500 pour toute Fault | HTTP 400 pour `Sender`, 500 pour `Receiver` |

---

## Partie B — Contract-first (`emprunt-contractfirst`)

### Étape 5 — Générer le code depuis le WSDL

`mvn generate-sources` produit dans `target/generated-sources/wsimport/mr/supnum/bib/contrat/` :
`EmpruntPortType`, `EmpruntService` (client), `EmprunterLivre`, `EmprunterLivreResponse`,
`EmpruntType`, `LivreIndisponible`, `LivreIndisponibleFault`, `ObjectFactory`, `package-info`.

**1. Opération, fault et action.**
Opération `emprunterLivre` (`portType EmpruntPortType`), message de fault
`bib:LivreIndisponibleFault` (fault nommée `LivreIndisponible`, partie `bib:LivreIndisponible`),
action SOAP `http://supnum.mr/bib/emprunterLivre` (`soap:operation/@soapAction` du binding).

**2. Signature dans `EmpruntPortType`.**
```java
@WebMethod(action = "http://supnum.mr/bib/emprunterLivre")
@WebResult(name = "emprunt", targetNamespace = "http://supnum.mr/bib")
public EmpruntType emprunterLivre(
    @WebParam(name = "idAdherent", targetNamespace = "http://supnum.mr/bib") String idAdherent,
    @WebParam(name = "isbn",       targetNamespace = "http://supnum.mr/bib") String isbn)
    throws LivreIndisponibleFault;
```
Le style *wrapped* « déballe » l'élément `emprunterLivre` en deux paramètres. `isbn` devient
un simple `String` : la facette `pattern` de `IsbnType` n'a pas d'équivalent en Java.

**3. Type de `dateRetour`.**
`XMLGregorianCalendar` (avec `@XmlSchemaType(name = "date")`), parce que le contrat déclare
`xs:date`. Dans la partie A, c'était l'inverse : le champ Java `String` avait produit `xs:string`.
Le type du contrat est maintenant précis, mais il faut convertir
(`DatatypeFactory.newXMLGregorianCalendar`).

**4. Classe d'exception générée.**
`LivreIndisponibleFault`. Le nom vient de l'attribut `name` du **`wsdl:message`** de fault
(`<wsdl:message name="LivreIndisponibleFault">`), et non du nom de la fault dans le `portType`
(`LivreIndisponible`). Le bean du `detail` s'appelle `LivreIndisponible`, d'après l'élément XSD.

### Étape 6 — Implémenter et comparer

`emprunter` avec `SOAPAction: "http://supnum.mr/bib/emprunterLivre"` : HTTP 200,
`<emprunt><numero>E-2026-0812</numero><dateRetour>2026-12-08</dateRetour></emprunt>`.
Avec `978-2-10-074444-4` : `S:Server — Livre indisponible`, `detail` =
`<LivreIndisponible xmlns="http://supnum.mr/bib"><isbn>978-2-10-074444-4</isbn><retourPrevu>2026-12-02</retourPrevu></LivreIndisponible>`.

**1. Le WSDL publié est-il le mien ?**
Oui. Il garde mes noms (`EmpruntSoapBinding`, `EmpruntPortType`, préfixes `wsdl:` et `xs:`), le
schéma est **inclus** dans `wsdl:types` (aucun `?xsd=1`) et contient
`<xs:pattern value="97[89]-\d-\d{2,5}-\d{2,6}-\d"/>`, `type="xs:date"` et
`elementFormDefault="qualified"`. Seule l'adresse `soap:address` est réécrite par le runtime.

**2. Ai-je dû modifier `01-emprunter.xml` ?**
Oui. Le schéma est `elementFormDefault="qualified"` : `idAdherent` et `isbn` doivent être dans
l'espace de noms `http://supnum.mr/bib`, donc écrits `<bib:idAdherent>` et `<bib:isbn>`
(version dans `emprunt-contractfirst/xml/`).

Avec l'ancien message (paramètres non qualifiés), le résultat dépend de la validation :
- **sans** `@SchemaValidation` : HTTP 200, mais les paramètres sont lus comme `null`. Preuve :
  envoyé avec l'ISBN `978-2-10-074444-4`, ce message obtient un emprunt au lieu de la Fault
  « Livre indisponible ». L'erreur est silencieuse.
- **avec** `@SchemaValidation` : `S:Client — cvc-complex-type.2.4.a: Invalid content was found
  starting with element 'idAdherent'. One of '{"http://supnum.mr/bib":idAdherent}' is expected.`

L'en-tête `SOAPAction` n'est pas vérifié par ce runtime : une valeur vide ou fausse donne aussi
HTTP 200. Le routage se fait sur l'élément du `Body`. Il faut tout de même l'envoyer, car le
contrat le déclare et d'autres serveurs l'exigent.

**3. ISBN `978-2-10`.**
- Sans `@SchemaValidation` : **accepté**, HTTP 200 et un emprunt créé. L'implémentation ne contrôle
  pas le format.
- Avec : **refusé** avant d'atteindre le code, HTTP 500 :
  `S:Client — org.xml.sax.SAXParseException; cvc-pattern-valid: Value '978-2-10' is not
  facet-valid with respect to pattern '97[89]-\d-\d{2,5}-\d{2,6}-\d' for type 'IsbnType'.`

  Le contrat est appliqué par le runtime, et le code n'a pas à réécrire la règle.

**4. Sans `wsdlLocation`.**
JAX-WS ne publie plus mon fichier : il **régénère** un WSDL à partir des annotations des classes
générées, comme en code-first.
- Le binding devient `EmpruntPortBinding` (au lieu de `EmpruntSoapBinding`).
- Le schéma est sorti dans `?xsd=1` (`xsd:import`).
- `isbn` redevient **`xs:string`** : le motif `IsbnType` est **perdu**, ainsi que les noms de types
  du contrat (2 437 octets contre 3 274).
- `xs:date` et `elementFormDefault="qualified"` survivent, car ils sont portés par les annotations
  JAXB.

Conséquence mesurée : même avec `@SchemaValidation`, l'ISBN `978-2-10` est de nouveau **accepté**
(HTTP 200), car la validation se fait contre ce schéma appauvri. `wsdlLocation` garantit que le
contrat publié est bien le contrat écrit.

---

## Conclusion — Choisir et rendre

**1. Deux différences entre le WSDL de la partie A et celui de la partie B.**
1. **Types** : A génère `xs:string` pour `isbn` et `dateRetour`, sans contrainte. B porte
   `IsbnType` (avec `xs:pattern`) et `xs:date`. Seul B peut être validé utilement.
2. **Qualification et organisation** : A est `unqualified`, avec le schéma externe (`?xsd=1`) et
   `soapAction=""`. B est `qualified`, avec le schéma inclus et
   `soapAction="http://supnum.mr/bib/emprunterLivre"`.

À noter aussi : A expose `rendreLivre`, que le contrat B ne déclare pas. Les noms diffèrent
également (`EmpruntPortBinding`/`EmpruntService` contre `EmpruntSoapBinding`/`EmpruntPortType`).

**2. Renommer `dateRetour` dans le code.**
Seul le projet **code-first** voit son contrat changer : le WSDL est régénéré à chaque démarrage, et
l'élément `<dateRetour>` change de nom sans que personne ne l'ait décidé. Les clients générés à
partir de l'ancien WSDL envoient ou attendent toujours `dateRetour`. JAXB ignore silencieusement
l'élément inconnu, donc le champ arrive `null` côté client, sans aucune erreur. C'est une rupture de
compatibilité invisible. En contract-first, le WSDL ne bouge pas ; renommer le champ Java casse la
compilation contre l'interface générée, et l'erreur apparaît chez le développeur, pas chez le client.

**3. Choix d'approche.**
- **Service Paiement** (Scolarité en .NET, Bibliothèque en PHP, portail en Node.js) :
  **contract-first**. Trois équipes et trois langages génèrent leurs clients depuis un même WSDL,
  qui doit être stable, versionné et revu comme une API publique. Les types précis
  (montants en `xs:decimal`, motifs, énumérations) et la validation par le schéma protègent un
  domaine sensible.
- **Prototype interne de deux jours** : **code-first**. Un seul consommateur, un contrat qui change
  souvent, et le WSDL généré suffit. Si le prototype part en production, on fige le WSDL généré
  et on passe en contract-first.
