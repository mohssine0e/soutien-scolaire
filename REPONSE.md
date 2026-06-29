# Exercice de selection — Plateforme de soutien scolaire avec messagerie

## 1. Comprehension du sujet

L'objectif est de construire une API back-end permettant a trois types d'acteurs
(eleve, enseignant, administrateur) d'interagir autour de demandes de soutien
scolaire : un eleve demande de l'aide dans une matiere, un enseignant consulte
les demandes disponibles et s'y affecte, et une messagerie simple permet
l'echange entre l'eleve et l'enseignant une fois la demande prise en charge.
L'accent est mis sur une architecture propre (controller/service/repository/
entity/DTO), une gestion des roles coherente, et une logique metier claire
(notamment sur les transitions de statut et les droits d'acces).

## 2. Choix techniques

- **Java 17 / Spring Boot 3.2.5** : version stable et largement utilisee, recommandee dans le sujet.
- **Spring Data JPA + H2 par defaut** : permet de lancer le projet sans aucune installation
  (base en memoire), avec un profil PostgreSQL fourni en alternative (`application-postgres.properties.example`).
- **Spring Security + JWT** : utilise comme recommande par le sujet plutot que justifie son absence.
  Le choix du JWT (plutot que des sessions) se justifie par la nature stateless d'une API REST :
  pas de session serveur a maintenir, le role est embarque dans le token et verifie a chaque requete.
- **DTO (records Java)** : separation stricte entre les entites JPA et ce qui transite par l'API,
  pour eviter les fuites de donnees sensibles (mot de passe) et decoupler le modele de persistance
  du contrat d'API.
- **@PreAuthorize (method security)** couple a une configuration `SecurityFilterChain` : double
  niveau de controle (au niveau des routes globales et au niveau fin de chaque endpoint).

## 3. Architecture du projet Spring Boot

```
entity/        -> User, Subject, SupportRequest, Message, Role, RequestStatus
repository/    -> interfaces Spring Data JPA (une par entite)
dto/           -> records pour les requetes/reponses
service/       -> AuthService, UserService, SubjectService, SupportRequestService,
                  MessageService, SecurityUtils (logique metier et controle des droits)
controller/    -> AuthController, UserController, SubjectController,
                  SupportRequestController, MessageController
security/      -> JwtUtil, JwtAuthFilter, CustomUserDetailsService
config/        -> SecurityConfig, DataSeeder (jeu de donnees de demo)
exception/     -> ResourceNotFoundException, BusinessRuleException, GlobalExceptionHandler
```

Le flux d'une requete : `Controller` (validation des DTO entrants, `@PreAuthorize`)
-> `Service` (logique metier, controle fin des droits, transitions de statut)
-> `Repository` (acces donnees) -> `Entity`.

## 4. Modele de donnees

- **User** : id, nom, email (unique), password (hash BCrypt), role (ELEVE/ENSEIGNANT/ADMIN), dateCreation.
- **Subject** (matiere) : id, nom (unique), description.
- **SupportRequest** (demande de soutien) : id, eleve (FK User), subject (FK Subject),
  enseignant (FK User, nullable jusqu'a affectation), description, status
  (CREEE/EN_COURS/TERMINEE/ANNULEE), dateCreation, dateMiseAJour.
- **Message** : id, supportRequest (FK), auteur (FK User), contenu, dateEnvoi.

Relations : `User 1—N SupportRequest` (en tant qu'eleve, et separement en tant
qu'enseignant), `Subject 1—N SupportRequest`, `SupportRequest 1—N Message`.

## 5. Endpoints REST developpes

| Methode | URL                                  | Role requis        | Description |
|---------|---------------------------------------|---------------------|-------------|
| POST    | /api/auth/register                    | public               | Inscription |
| POST    | /api/auth/login                       | public               | Connexion, retourne un JWT |
| GET     | /api/users                            | ADMIN                | Liste des utilisateurs (filtrable par role) |
| GET     | /api/users/{id}                       | ADMIN                | Detail d'un utilisateur |
| DELETE  | /api/users/{id}                       | ADMIN                | Supprimer un utilisateur |
| GET     | /api/subjects                         | authentifie          | Liste des matieres |
| GET     | /api/subjects/{id}                    | authentifie          | Detail d'une matiere |
| POST    | /api/subjects                         | ADMIN                | Creer une matiere |
| PUT     | /api/subjects/{id}                    | ADMIN                | Modifier une matiere |
| DELETE  | /api/subjects/{id}                    | ADMIN                | Supprimer une matiere |
| POST    | /api/requests                         | ELEVE                | Creer une demande de soutien |
| GET     | /api/requests/disponibles             | ENSEIGNANT, ADMIN     | Demandes non affectees |
| POST    | /api/requests/{id}/affecter            | ENSEIGNANT            | S'affecter a une demande |
| PATCH   | /api/requests/{id}/statut              | concerne par la demande | Changer le statut |
| GET     | /api/requests/mes-demandes             | authentifie           | Demandes selon mon role |
| GET     | /api/requests/{id}                    | concerne par la demande | Detail d'une demande |
| POST    | /api/requests/{requestId}/messages     | concerne par la demande | Envoyer un message |
| GET     | /api/requests/{requestId}/messages     | concerne par la demande | Historique des messages |

Voir `requests-exemples.http` pour des exemples de requetes completes.

## 6. Gestion des roles et des droits

Trois roles : `ELEVE`, `ENSEIGNANT`, `ADMIN`, stockes dans l'entite `User` et
encodes dans le JWT a la connexion. Deux niveaux de controle :

1. **Au niveau des routes** (`SecurityConfig`) : regles generales par prefixe d'URL
   (ex. `/api/users/**` reserve a ADMIN).
2. **Au niveau des methodes** (`@PreAuthorize` sur les controllers) : regles precises
   par endpoint (ex. seul un ELEVE peut `POST /api/requests`).
3. **Au niveau metier** (`SupportRequestService.verifierDroitsSurDemande`) : controle
   fin que l'utilisateur agit bien sur une ressource qui le concerne (son eleve, ou
   l'enseignant affecte, ou un admin) — un role correct ne suffit pas, il faut aussi
   etre le bon utilisateur.

## 7. Gestion des demandes de soutien scolaire

Cycle de vie d'une demande :
1. Un eleve cree une demande (`status = CREEE`, pas d'enseignant affecte).
2. Un enseignant consulte `/api/requests/disponibles` (demandes `CREEE` sans enseignant).
3. L'enseignant s'affecte (`POST /{id}/affecter`) -> `status` passe a `EN_COURS`.
4. Le statut evolue ensuite vers `TERMINEE` ou `ANNULEE` (statuts finaux).

Les transitions sont controlees explicitement dans `SupportRequestService` :
`CREEE -> EN_COURS | ANNULEE`, `EN_COURS -> TERMINEE | ANNULEE`. Toute autre
transition (ex. relancer une demande `TERMINEE`) leve une `BusinessRuleException`
(HTTP 409).

## 8. Messagerie entre eleve et enseignant

La messagerie est rattachee a une demande (`SupportRequest`) et n'est activable
qu'une fois un enseignant affecte (sinon `BusinessRuleException`, HTTP 409) — cela
evite qu'un eleve puisse contacter un enseignant hors du cadre d'une demande prise
en charge. Seuls l'eleve createur et l'enseignant affecte (ou un admin en lecture)
peuvent envoyer/lire les messages, via le meme controle `verifierDroitsSurDemande`
que pour les demandes. L'historique est trie chronologiquement (`dateEnvoi ASC`).

## 9. Gestion des erreurs et validations

- Validation des DTO entrants via `jakarta.validation` (`@NotBlank`, `@Email`, `@Size`, `@NotNull`).
- `GlobalExceptionHandler` (`@RestControllerAdvice`) centralise toutes les erreurs et
  retourne un JSON homogene (`ErrorResponse`) avec le code HTTP adapte :
  - 400 : validation invalide (`MethodArgumentNotValidException`)
  - 401 : identifiants incorrects
  - 403 : acces refuse (role insuffisant)
  - 404 : ressource introuvable (`ResourceNotFoundException`)
  - 409 : violation d'une regle metier (`BusinessRuleException`) — email deja utilise,
    transition de statut invalide, demande deja affectee, etc.
  - 500 : erreur inattendue (filet de securite)

## 10. Tests realises

- **Tests unitaires** (Mockito) sur la logique metier critique :
  - `SupportRequestServiceTest` : creation de demande (role correct/incorrect),
    affectation d'un enseignant (succes, echec si deja affectee), transitions de
    statut (valide/invalide), controle des droits d'acces.
  - `MessageServiceTest` : envoi de message (succes si enseignant affecte, echec sinon,
    echec si utilisateur non autorise).
- **Test d'integration** (`AuthControllerIT`, MockMvc + contexte Spring complet avec H2) :
  inscription puis connexion, rejet d'un email deja utilise, rejet d'un email invalide.

Lancement : `mvn test`.

## 11. Documentation d'installation et d'utilisation

Voir `README.md` (installation H2/PostgreSQL, comptes de demonstration, tableau des
roles/droits, liste des endpoints) et `requests-exemples.http` (exemples de requetes
pretes a l'emploi, y compris en curl).

## 12. Difficultes rencontrees

- **Modeliser la disponibilite d'un enseignant pour une demande** : choix retenu —
  une demande "disponible" est `status = CREEE` ET `enseignant IS NULL`, ce qui evite
  d'ajouter un champ redondant et garde le statut comme unique source de verite.
- **Articulation entre roles Spring Security et regles metier fines** : `@PreAuthorize`
  suffit pour verifier le role, mais pas pour verifier qu'un enseignant agit sur *sa*
  demande — d'ou l'ajout d'un controle explicite dans le service (`verifierDroitsSurDemande`),
  sans quoi n'importe quel enseignant aurait pu changer le statut de n'importe quelle demande.
- **Eviter les transitions de statut incoherentes** : plutot que de laisser n'importe
  quel statut etre pose librement, une matrice de transitions valides a ete formalisee
  pour empecher par exemple de repasser une demande `TERMINEE` a `EN_COURS`.

## 13. Ce que j'ai fait avec l'aide de l'IA

J'ai utilise Claude (Anthropic) pour generer la premiere version complete du squelette
du projet : structure des packages, entites JPA, repositories, DTO, configuration
Spring Security/JWT, controllers, et le jeu de tests initial. L'IA m'a egalement aide a
redacter le README et les exemples de requetes HTTP, ainsi qu'a structurer ce document
de rendu selon l'ossature imposee.

## 14. Ce qui releve de mon travail personnel

- J'ai relu entierement le code genere, verifie la coherence des packages, des
  annotations JPA et des regles de securite, et corrige 
- J'ai execute le projet en local avec `mvn spring-boot:run`, teste manuellement les
  scenarios cles via les exemples de requetes (creation de demande, affectation,
  changement de statut, envoi de messages) et corrige les anomalies rencontrees.
- J'ai pris les decisions de conception suivantes par moi-meme : 


## 15. Conclusion et pistes d'amelioration

Le projet repond a l'ensemble des exigences fonctionnelles et techniques du sujet :
gestion des roles, cycle de vie complet d'une demande de soutien, messagerie
conditionnee a l'affectation, securisation par JWT, validation et gestion d'erreurs
centralisee, tests unitaires et d'integration, documentation complete.

Pistes d'amelioration possibles :
- Pagination des listes (demandes, messages) pour un usage a grande echelle.
- Notifications (email ou WebSocket) lors d'un nouveau message ou d'une affectation.
- Tests d'integration complementaires couvrant l'ensemble des controllers (demandes, messages).
- Refresh tokens / expiration plus fine de la session JWT.
- Pagination et recherche full-text sur les matieres pour une grande liste.
