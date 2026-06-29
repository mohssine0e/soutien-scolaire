# Plateforme de soutien scolaire avec messagerie

API REST Spring Boot permettant de gerer des demandes de soutien scolaire entre
eleves, enseignants et administrateurs, avec messagerie integree.

## Stack technique

- Java 17
- Spring Boot 3.2.5 (Spring Web, Spring Data JPA, Spring Security, Spring Validation)
- Base de donnees : H2 (en memoire, par defaut) ou PostgreSQL (optionnel)
- Authentification : JWT (jjwt 0.11.5)
- Tests : JUnit 5, Mockito, MockMvc
- Maven

## Architecture du projet

```
src/main/java/com/soutien/
 ├── entity/        Entites JPA (User, Subject, SupportRequest, Message, Role, RequestStatus)
 ├── repository/    Interfaces Spring Data JPA
 ├── dto/           Records pour les requetes/reponses (separation API <-> entites)
 ├── service/        Logique metier (regles de gestion, controle des droits)
 ├── controller/    Endpoints REST
 ├── security/      JWT (generation/validation) + UserDetailsService + filtre
 ├── config/        Configuration Spring Security + jeu de donnees de demo
 └── exception/     Exceptions metier + gestionnaire global des erreurs
```

## Installation et lancement

### Prerequis
- JDK 17+
- Maven 3.8+

### Lancer avec H2 (par defaut, sans rien installer)

```bash
mvn spring-boot:run
```

L'application demarre sur `http://localhost:8080`.
Un jeu de donnees de demonstration est cree automatiquement au demarrage (voir `DataSeeder`) :

| Email                 | Mot de passe   | Role        |
|------------------------|----------------|-------------|
| admin@soutien.ma       | password123    | ADMIN       |
| eleve@soutien.ma       | password123    | ELEVE       |
| enseignant@soutien.ma  | password123    | ENSEIGNANT  |

Trois matieres sont aussi creees : Mathematiques, Physique, Informatique.

La console H2 est accessible sur `http://localhost:8080/h2-console`
(JDBC URL : `jdbc:h2:mem:soutiendb`, user `sa`, mot de passe vide).

### Lancer avec PostgreSQL

1. Creer une base : `CREATE DATABASE soutiendb;`
2. Renommer `src/main/resources/application-postgres.properties.example` en
   `application.properties` (en remplacant celui pour H2), et adapter user/password.
3. `mvn spring-boot:run`

### Lancer les tests

```bash
mvn test
```

## Authentification

Toutes les routes (sauf `/api/auth/**`) necessitent un header :

```
Authorization: Bearer <token>
```

Le token est obtenu via `/api/auth/login` ou `/api/auth/register`.

## Roles et droits (resume)

| Action                                          | ELEVE | ENSEIGNANT | ADMIN |
|--------------------------------------------------|:-----:|:----------:|:-----:|
| Creer une demande de soutien                     |  ✅   |     ❌     |  ❌   |
| Consulter les demandes disponibles               |  ❌   |     ✅     |  ✅   |
| S'affecter a une demande                         |  ❌   |     ✅     |  ❌   |
| Changer le statut d'une demande (qui la concerne)|  ✅   |     ✅     |  ✅   |
| Gerer les matieres (creer/modifier/supprimer)    |  ❌   |     ❌     |  ✅   |
| Gerer les utilisateurs                           |  ❌   |     ❌     |  ✅   |
| Envoyer/lire des messages (sur une demande affectee, si concerne) | ✅ | ✅ | ✅ (lecture) |

## Endpoints principaux

Voir `requests-exemples.http` pour des exemples complets de requetes a executer
(curl, ou directement avec l'extension "REST Client" de VS Code / IntelliJ HTTP Client).

- `POST /api/auth/register` — inscription
- `POST /api/auth/login` — connexion
- `GET /api/subjects` — liste des matieres
- `POST /api/subjects` (ADMIN) — creer une matiere
- `POST /api/requests` (ELEVE) — creer une demande de soutien
- `GET /api/requests/disponibles` (ENSEIGNANT/ADMIN) — demandes non affectees
- `POST /api/requests/{id}/affecter` (ENSEIGNANT) — s'affecter a une demande
- `PATCH /api/requests/{id}/statut` — changer le statut (CREEE/EN_COURS/TERMINEE/ANNULEE)
- `GET /api/requests/mes-demandes` — mes demandes selon mon role
- `POST /api/requests/{requestId}/messages` — envoyer un message lie a une demande
- `GET /api/requests/{requestId}/messages` — historique des messages de la demande

## Notes de conception

- Les transitions de statut sont controlees cote serveur :
  `CREEE -> EN_COURS | ANNULEE`, `EN_COURS -> TERMINEE | ANNULEE`. Les statuts
  `TERMINEE` et `ANNULEE` sont finaux.
- La messagerie n'est activee qu'une fois un enseignant affecte a la demande.
- Seuls l'eleve createur, l'enseignant affecte ou un admin peuvent consulter/agir
  sur une demande et ses messages.
