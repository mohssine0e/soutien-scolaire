# Guide de relecture — comprends chaque étape avant de rendre le projet

Ce document n'est PAS à rendre au recruteur. C'est un guide pour TOI : il
t'explique chaque couche du projet, dans l'ordre où les comprendre, avec les
concepts derrière chaque choix. Une fois que tu peux réexpliquer chaque section
avec tes propres mots, tu es prêt à remplir honnêtement les sections 13/14 du
REPONSE.md.

Coche mentalement chaque case en lisant le code correspondant en parallèle.

---

## Étape 0 — Vue d'ensemble : comment une requête traverse l'app

Avant le détail, le trajet complet d'une requête HTTP, par ex. "un élève crée
une demande de soutien" :

```
Client (Postman/curl)
   │  POST /api/requests + JWT dans le header Authorization
   ▼
JwtAuthFilter           → lit le token, identifie l'utilisateur, le met dans le "contexte de sécurité"
   ▼
Spring Security         → vérifie que le rôle a le droit d'accéder à cette route (@PreAuthorize)
   ▼
SupportRequestController → reçoit le JSON, le valide (@Valid), appelle le service
   ▼
SupportRequestService    → logique métier : qui suis-je ? ai-je le droit ? quelle règle s'applique ?
   ▼
SupportRequestRepository → traduit en SQL via Spring Data JPA
   ▼
Base H2/PostgreSQL
   ▼
... la réponse remonte, transformée en DTO avant d'être renvoyée en JSON
```

**Concept clé : séparation des responsabilités.** Chaque couche a un seul
travail. Le controller ne contient JAMAIS de logique métier (pas de `if
(user.role == ELEVE)`) — il délègue tout au service. C'est ce qui rend le code
testable : on peut tester `SupportRequestService` sans avoir besoin d'un vrai
serveur HTTP (voir les tests Mockito).

---

## Étape 1 — Les entités (`entity/`)

Fichiers : `User.java`, `Subject.java`, `SupportRequest.java`, `Message.java`,
`Role.java`, `RequestStatus.java`.

**Concept : une entité JPA = une table en base.** Chaque champ devient une
colonne. `@Entity` dit à Spring "cette classe doit être persistée".
`@Id` + `@GeneratedValue` = la clé primaire auto-incrémentée.

À vérifier en lisant `User.java` :
- `@Enumerated(EnumType.STRING)` sur `role` : pourquoi STRING et pas ORDINAL ?
  → Parce que si tu réordonnes l'enum `Role` plus tard (ex. ajouter un rôle au
  milieu), un stockage ORDINAL (0,1,2) casserait toutes les données existantes.
  STRING stocke "ELEVE", "ENSEIGNANT" — illisible mais sûr.
- `password` stocke un **hash BCrypt**, jamais le mot de passe en clair (vois
  `AuthService.register` : `passwordEncoder.encode(...)`).

À vérifier dans `SupportRequest.java` :
- `@ManyToOne` vers `User` (deux fois : `eleve` et `enseignant`) et vers
  `Subject`. Comprends bien : une demande appartient à UN élève, est dans UNE
  matière, et est affectée à AU PLUS UN enseignant (`enseignant` est nullable
  — `@JoinColumn(name = "enseignant_id")` sans `nullable = false`).
- Le champ `status` utilise l'enum `RequestStatus` (CREEE/EN_COURS/TERMINEE/ANNULEE).

**Question à te poser** : pourquoi `Message` a une `@ManyToOne` vers
`SupportRequest` et pas l'inverse (`SupportRequest` avec une liste de
messages) ? → Choix volontaire de simplicité : on n'a jamais besoin de charger
tous les messages en même temps qu'une demande, donc pas de `@OneToMany`
bidirectionnel inutile (qui complique les performances et les sérialisations JSON).

---

## Étape 2 — Les repositories (`repository/`)

Fichiers : `UserRepository`, `SubjectRepository`, `SupportRequestRepository`,
`MessageRepository`.

**Concept : Spring Data JPA génère le SQL pour toi.** Tu écris juste une
interface avec des méthodes nommées selon une convention, et Spring devine la
requête.

Exemple concret dans `SupportRequestRepository` :
```java
List<SupportRequest> findByStatusAndEnseignantIsNull(RequestStatus status);
```
Spring traduit ça en :
```sql
SELECT * FROM support_requests WHERE status = ? AND enseignant_id IS NULL
```
C'est exactement la requête qui répond à "quelles demandes sont disponibles
pour un enseignant ?" (statut CREEE + pas encore affecté).

**À vérifier** : relis chaque méthode de chaque repository et essaie de
deviner le SQL généré avant de regarder la doc Spring Data JPA. Si tu n'arrives
pas à le deviner, c'est le signe qu'il faut comprendre cette convention de
nommage (`findBy`, `And`, `OrderBy...Asc`, `IsNull`, etc.) avant l'entretien.

---

## Étape 3 — Les DTO (`dto/`)

**Concept : ne jamais exposer une entité JPA directement dans l'API.**

Deux raisons :
1. **Sécurité** : si `UserController` renvoyait l'entité `User` brute, le champ
   `password` (même hashé) partirait dans la réponse JSON. `UserResponse` ne
   contient QUE ce qui doit être visible.
2. **Découplage** : le contrat d'API (ce que le client voit) peut rester stable
   même si tu changes la structure interne de la base de données.

Regarde la paire `SupportRequestCreateRequest` (ce que le client ENVOIE) vs
`SupportRequestResponse` (ce que l'API RENVOIE) — ce sont deux objets
différents, avec des champs différents, même si les deux tournent autour de la
même entité `SupportRequest`.

**Pourquoi des `record` et pas des classes classiques ?** Un `record` Java
(depuis Java 14+) génère automatiquement le constructeur, les getters,
`equals()`, `hashCode()`, `toString()`. Pour un objet immuable qui ne fait que
transporter des données (un DTO), c'est exactement ce qu'on veut — moins de
code répétitif que Lombok `@Data` sur une classe.

**À vérifier** : dans `SupportRequestResponse.fromEntity(...)`, comprends
pourquoi on "aplatit" les objets liés (`eleveNom` au lieu de renvoyer l'objet
`User` complet). C'est pour éviter d'exposer toute l'entité `User` (et son
mot de passe hashé) imbriquée dans la réponse.

---

## Étape 4 — La validation (`@NotBlank`, `@Email`, etc.)

**Concept : valider les données AVANT qu'elles n'atteignent la logique métier.**

Dans `RegisterRequest` :
```java
@Email(message = "Format d'email invalide")
String email,
```
Quand le controller a `@Valid @RequestBody RegisterRequest request`, Spring
vérifie automatiquement ces annotations et lève une
`MethodArgumentNotValidException` si ça échoue — AVANT même d'entrer dans le
corps de la méthode du controller.

**À vérifier** : suis le chemin complet d'une validation échouée → elle est
attrapée dans `GlobalExceptionHandler.handleValidation(...)` → transformée en
JSON avec le code HTTP 400 et la liste des champs en erreur (`fieldErrors`).

---

## Étape 5 — La sécurité : Spring Security + JWT (`security/`, `config/SecurityConfig.java`)

C'est la partie la plus dense. Prends ton temps ici — c'est ce qu'on te
demandera très probablement d'expliquer en entretien.

### 5.1 — Le principe du JWT

Un **JWT (JSON Web Token)** est une chaîne signée contenant des informations
(ici : l'email de l'utilisateur, son id, son rôle, une date d'expiration). Le
serveur le signe avec une clé secrète (`jwt.secret`). N'importe qui peut LIRE
le contenu du token (il n'est pas chiffré, juste encodé en base64), mais
personne ne peut le MODIFIER sans casser la signature — donc le serveur peut
faire confiance à son contenu sans avoir besoin de stocker de session.

**Concept clé : stateless.** Contrairement à une session classique (où le
serveur garde en mémoire "qui est connecté"), avec JWT le serveur ne mémorise
RIEN entre deux requêtes. Toute l'info nécessaire est dans le token que le
client renvoie à chaque fois. C'est pour ça que
`SecurityConfig` a :
```java
.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

### 5.2 — Le cycle de vie du token

1. `POST /api/auth/login` → `AuthService.login()` vérifie email/mot de passe
   (`authenticationManager.authenticate(...)`, qui utilise
   `CustomUserDetailsService` + `BCryptPasswordEncoder` en coulisses) → si OK,
   `JwtUtil.generateToken(...)` crée et signe le token.
2. Le client stocke ce token et le renvoie dans le header
   `Authorization: Bearer <token>` sur CHAQUE requête suivante.
3. `JwtAuthFilter` (un filtre exécuté avant que la requête n'atteigne le
   controller) lit ce header, extrait le token, vérifie sa validité
   (`jwtUtil.isTokenValid(...)`), et si tout va bien, place l'utilisateur dans
   le `SecurityContextHolder` — c'est cette étape qui permet à
   `SecurityUtils.getCurrentUser()` de savoir "qui fait cette requête" plus
   loin dans le service.

**À vérifier** : ouvre `JwtAuthFilter.java` et suis ligne par ligne. Pose-toi
la question : que se passe-t-il si le header `Authorization` est absent ? (→
`filterChain.doFilter` est appelé directement, la requête continue SANS
authentification — et c'est Spring Security qui bloquera plus loin si la route
est protégée).

### 5.3 — Les deux niveaux d'autorisation

1. **Niveau route**, dans `SecurityConfig.securityFilterChain()` :
   ```java
   .requestMatchers("/api/users/**").hasRole("ADMIN")
   ```
   Règle large, par préfixe d'URL.

2. **Niveau méthode**, avec `@PreAuthorize` directement sur les méthodes des
   controllers :
   ```java
   @PostMapping
   @PreAuthorize("hasRole('ELEVE')")
   public ResponseEntity<SupportRequestResponse> creer(...)
   ```
   Plus précis : seule cette méthode-là est restreinte, indépendamment du préfixe.

**Pourquoi les deux ?** Le niveau route protège "par défaut" même si tu oublies
une annotation. Le niveau méthode permet une granularité fine (deux méthodes
sur la même URL avec des verbes HTTP différents peuvent avoir des règles
différentes).

### 5.4 — Pourquoi `@PreAuthorize("hasRole('ELEVE')")` ne suffit PAS toujours

Relis `SupportRequestService.verifierDroitsSurDemande(...)`. `@PreAuthorize`
vérifie le RÔLE, mais pas l'IDENTITÉ précise. Un enseignant a le droit de
changer le statut d'une demande — mais seulement de SA demande, pas de
n'importe quelle demande affectée à un collègue. C'est une règle métier qui ne
peut pas s'exprimer avec une simple annotation de rôle : il faut comparer
`demande.getEnseignant().getId()` avec l'id de l'utilisateur courant. C'est
exactement le rôle de cette méthode.

**Comprends bien cette distinction : "authentification" (qui es-tu) vs
"autorisation par rôle" (que peux-tu faire en général) vs "autorisation
métier" (peux-tu faire CETTE action sur CETTE ressource précise).**

---

## Étape 6 — La logique métier (`service/`)

### 6.1 — `SupportRequestService` : le cœur du projet

C'est le fichier à maîtriser parfaitement avant l'entretien. Trois mécanismes
à bien comprendre :

**a) La matrice de transitions de statut**
```java
private static final Set<RequestStatus> FROM_CREEE = Set.of(RequestStatus.EN_COURS, RequestStatus.ANNULEE);
private static final Set<RequestStatus> FROM_EN_COURS = Set.of(RequestStatus.TERMINEE, RequestStatus.ANNULEE);
```
Au lieu de laisser n'importe quel statut être posé n'importe comment, on
définit explicitement quelles transitions sont valides. `verifierTransition`
utilise un `switch` exhaustif sur l'enum — si tu ajoutes un jour un nouveau
statut, le compilateur Java t'OBLIGE à traiter ce nouveau cas (c'est l'intérêt
des `switch` exhaustifs sur enum en Java moderne).

**b) Le critère de "demande disponible"**
```java
supportRequestRepository.findByStatusAndEnseignantIsNull(RequestStatus.CREEE)
```
Pas de champ booléen "disponible" redondant — la disponibilité se déduit de
deux champs déjà existants (`status` + `enseignant`). C'est un principe de
conception important : **éviter de dupliquer une information qu'on peut déduire**,
sinon les deux informations peuvent un jour se contredire (bug classique).

**c) Le contrôle d'accès aux ressources** (déjà vu à l'étape 5.4).

### 6.2 — `MessageService`

Remarque la dépendance : `MessageService` appelle
`supportRequestService.verifierDroitsSurDemande(...)` — il RÉUTILISE la
logique d'autorisation du service des demandes plutôt que de la dupliquer.
C'est le principe **DRY (Don't Repeat Yourself)**.

Vérifie bien la règle : un message ne peut être envoyé QUE si
`demande.getEnseignant() != null` — autrement dit, la messagerie n'existe que
pour les demandes déjà prises en charge. Relie ça à la consigne du sujet :
"messagerie simple entre un élève et un enseignant" — il faut qu'il y ait
déjà un enseignant identifié pour qu'une conversation ait un sens.

### 6.3 — `AuthService`

Repère les trois étapes de `register()` :
1. Vérifier que l'email n'existe pas déjà (`existsByEmail`) → sinon `BusinessRuleException`.
2. Hasher le mot de passe (`passwordEncoder.encode`) avant de sauvegarder.
3. Générer immédiatement un token pour que l'utilisateur soit connecté dès l'inscription.

---

## Étape 7 — Les controllers (`controller/`)

**Concept : un controller est "fin".** Il ne fait que :
1. Recevoir/valider l'entrée (`@Valid @RequestBody`).
2. Vérifier le rôle global (`@PreAuthorize`).
3. Appeler UNE méthode de service.
4. Choisir le code HTTP de la réponse (`ResponseEntity.status(HttpStatus.CREATED)`).

**À vérifier** : compare `POST /api/requests` (status 201 CREATED, car on crée
une ressource) avec `PATCH /api/requests/{id}/statut` (status 200 OK par
défaut, car on modifie une ressource existante). Sais-tu expliquer la
différence entre PUT, PATCH et POST côté HTTP ? (POST = créer / action ;
PUT = remplacer entièrement une ressource ; PATCH = modifier partiellement).

---

## Étape 8 — La gestion des erreurs (`exception/`)

**Concept : centraliser la transformation exception → réponse HTTP.**

Sans `@RestControllerAdvice`, chaque controller devrait avoir son propre
try/catch pour chaque type d'erreur — répétitif et source d'incohérences (un
controller renverrait peut-être un 500 là où un autre renverrait un 400 pour
la même erreur).

`GlobalExceptionHandler` intercepte les exceptions à un seul endroit, pour
TOUTE l'application. Le `@ExceptionHandler(BusinessRuleException.class)`
attrape spécifiquement cette exception n'importe où elle est levée (même
profondément dans un service) et la transforme en réponse 409 cohérente.

**À vérifier** : trouve dans le code TROIS endroits différents où
`BusinessRuleException` est levée (`AuthService`, `SupportRequestService`,
`MessageService`...) et vérifie que dans les trois cas, le client recevra
bien un 409 avec un message clair — sans qu'aucun de ces trois services
n'ait eu besoin d'écrire du code de réponse HTTP lui-même. C'est la preuve
que la séparation fonctionne.

---

## Étape 9 — Les tests (`src/test/`)

### 9.1 — Tests unitaires (Mockito) : `SupportRequestServiceTest`, `MessageServiceTest`

**Concept : tester une classe ISOLÉMENT, sans dépendre des autres.**

```java
@Mock private SupportRequestRepository supportRequestRepository;
@InjectMocks
private SupportRequestService supportRequestService;
```
`@Mock` crée un faux objet qui ne fait RIEN par défaut. `when(...).thenReturn(...)`
lui dit quoi répondre quand on l'appelle. `@InjectMocks` injecte ces faux
objets dans le service qu'on teste réellement. Résultat : on teste UNIQUEMENT
la logique de `SupportRequestService`, sans jamais toucher une vraie base de
données — les tests sont rapides et fiables.

**À vérifier** : relis `affecterEnseignant_doitEchouer_siDemandeDejaAffectee`
et trouve la ligne qui "prépare le mensonge" (le mock retourne une demande
déjà affectée) puis la ligne qui vérifie que le service refuse bien
(`assertThrows`).

### 9.2 — Test d'intégration : `AuthControllerIT`

**Concept : tester le système complet, avec un vrai (mais temporaire) contexte
Spring et une vraie base H2 en mémoire.** `@SpringBootTest` démarre toute
l'application. `MockMvc` simule une requête HTTP sans avoir besoin d'un
serveur réel qui écoute sur un port. C'est plus lent qu'un test unitaire mais
ça vérifie que TOUTES les couches (sécurité, validation, controller, service,
base) fonctionnent ensemble.

---

## Étape 10 — Le `DataSeeder`

Juste un `CommandLineRunner` : Spring exécute sa méthode `run()`
automatiquement une fois au démarrage. Utile uniquement pour avoir des comptes
de test immédiatement disponibles avec H2 (qui est vide à chaque redémarrage,
contrairement à PostgreSQL qui garde les données).

---

## Checklist finale avant de rendre

Pour chaque ligne, dis-toi "je pourrais l'expliquer à l'oral" :

- [ ] Je sais expliquer pourquoi on utilise des DTO plutôt que les entités directement.
- [ ] Je sais expliquer le rôle exact de `JwtAuthFilter` et du `SecurityContextHolder`.
- [ ] Je sais expliquer la différence entre rôle (`@PreAuthorize`) et autorisation métier (`verifierDroitsSurDemande`).
- [ ] Je sais expliquer pourquoi la disponibilité d'une demande ne stocke pas un booléen dédié.
- [ ] Je sais expliquer la matrice de transitions de statut et pourquoi un `switch` exhaustif est utile ici.
- [ ] Je sais expliquer la différence entre un test unitaire (Mockito) et un test d'intégration (MockMvc + @SpringBootTest).
- [ ] J'ai lancé `mvn spring-boot:run` et testé moi-même au moins 3 scénarios via `requests-exemples.http`.
- [ ] J'ai relu et personnalisé les sections 13 et 14 du `REPONSE.md` avec mes propres mots.
