# 🕐 Gestion Pointage & Congés — Application Android (Java + XML)

Application mobile Android développée dans le cadre du **TP d'Android Studio** —
ISET Sidi Bouzid, Département Informatique, année 2026-2027.

Elle permet de gérer le **pointage des employés** (entrée, sortie, pauses),
les **demandes de congés** via un **agenda**, et un **espace administrateur**
pour valider les congés et consulter les pauses de chaque utilisateur.

---

## ✅ Fonctionnalités

### 1. Authentification
| Compte | E-mail | Mot de passe |
|---|---|---|
| Employé | `employee@gmail.com` | `emp123` |
| Employé | `sarra@gmail.com` / `mohamed@gmail.com` / `nour@gmail.com` / `karim@gmail.com` | `emp123` |
| Administrateur | `admin@gmail.com` | `admin123` |

- Connexion **employé** et connexion **administrateur** séparées (puces de rôle).
- Mots de passe stockés **cryptés en SHA-256**.
- Session conservée (pas de reconnexion à chaque ouverture).
- Afficher / masquer le mot de passe, messages d'erreur clairs.

### 2. Espace employé
- **Pointage** : bouton *Pointer l'arrivée* puis *Pointer la sortie*.
  Horloge en temps réel, statut du jour (Non pointé / En poste / Terminé).
- **Pauses** : choix du **type de pause** dans un dialogue
  (🍽 Déjeuner, ☕ Café, 💼 Personnelle, 🏥 Médicale, 🕌 Prière),
  durée calculée en direct, puis *Reprendre le travail*.
  Impossible de pointer la sortie tant qu'une pause est ouverte.
- **Historique** : toutes les journées, heure d'arrivée, heure de sortie,
  durée de travail et **pauses de chaque journée**.
- **Agenda & congés** : calendrier mois par mois, sélection début/fin,
  nombre de jours calculé, jours fériés affichés, puis envoi de la demande.

### 3. Espace administrateur
- **Tableau de bord** : nb d'employés, présents du jour, congés en attente,
  pauses du jour.
- **Employés** : liste + recherche → clic = **détail** avec pointages,
  sorties et **pauses de chaque utilisateur**.
- **Pauses du jour** : toutes les pauses de la journée, par employé,
  avec type, début, fin, durée, statut (en cours / terminée).
- **Demandes de congé** : filtres *Tous / En attente / Acceptés / Refusés*,
  boutons **Accepter** et **Refuser** avec confirmation.

---

## 🌍 API externe (exigence du barème)

L'application consomme l'API publique **[Nager.Date](https://date.nager.at/)**
avec **Retrofit + Gson** :

```
GET https://date.nager.at/api/v3/PublicHolidays/{année}/{pays}
```

- Les **jours fériés** sont affichés en jaune dans l'agenda.
- Un rappel s'affiche si la demande tombe sur un jour férié.
- L'écran administrateur affiche le nombre de jours fériés de l'année
  et le **prochain** jour férié.

---

## 🗄 Architecture

```
app/src/main/java/com/example/gestionpointage/
├── SplashActivity              → écran de démarrage + redirection selon la session
├── LoginActivity               → connexion employé / administrateur
├── EmployeeDashboardActivity   → tableau de bord employé
├── PointageActivity            → entrée / sortie + pauses du jour
├── HistoryActivity             → historique des pointages
├── LeaveActivity               → agenda (calendrier) + mes demandes
├── AddLeaveActivity            → formulaire d'envoi d'une demande
├── AdminDashboardActivity      → tableau de bord administrateur
├── AdminUsersActivity          → liste + recherche des employés
├── UserDetailActivity          → pointages & pauses d'un employé
├── AdminLeavesActivity         → accepter / refuser les congés
├── AdminBreaksActivity         → pauses de tous les employés
├── database/DatabaseHelper     → base SQLite (users, attendance, breaks, leaves)
├── model/                      → User, AttendanceRecord, BreakRecord, LeaveRequest
├── api/                        → Retrofit : HolidayApi, HolidayService, ApiClient
├── adapter/                    → RecyclerView : Break, Attendance, Leave, Employee, Day
├── session/SessionManager      → SharedPreferences (session connectée)
└── util/Ui                     → formats de dates, toasts
```

**Base SQLite locale** `gestion_pointage.db` :

| Table | Rôle |
|---|---|
| `users` | comptes (admin / employee), nom, e-mail, poste, mot de passe hashé |
| `attendance` | un pointage par jour : `arrival_time`, `exit_time` |
| `breaks` | pauses liées au pointage : `type`, `start_time`, `end_time` |
| `leaves` | demandes : type, dates, motif, statut, décideur |

---

## 🎨 Design (le « CSS » d'Android)

Android n'a pas de CSS : le style est géré par 3 fichiers XML :

| Fichier | Rôle |
|---|---|
| `res/values/colors.xml` | palette (bleus, vert, orange, rouge, violet) |
| `res/values/themes.xml` | thème Material 3 + styles réutilisés |
| `res/drawable/*.xml` | cartes arrondies, boutons dégradés, pastilles de statut, icônes vectorielles |
| `res/layout/*.xml` | mise en page de chaque écran |

- Dégradés bleus sur les en-têtes et la page de connexion
- Cartes blanches arrondies (20dp) avec ombre
- Pastilles colorées : 🟠 en attente, 🟢 accepté, 🔴 refusé
- Calendrier entièrement personnalisé (RecyclerView 7 colonnes)

---

## 🛠 Construction du projet

```bash
# APK de debug (installable)
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# APK de livraison signé (release)
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Clé de signature : `keystore/gestionpointage.jks`
(alias `gestionpointage` — mot de passe `gestionpointage`).

---

## 📦 Livrables

- [x] Code source publié sur GitHub avec des **commits réguliers**
- [x] **APK installable** (debug + release signé)
- [x] **API externe** consommée (Retrofit — jours fériés)
- [x] Application **fonctionnelle** et testée sur émulateur
