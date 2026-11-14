# Filothèque

Application Android native de gestion d'inventaire de fils à broder (DMC, Anchor et marques personnalisées) conçue pour la création de bracelets brésiliens.

## Fonctionnalités

- **Nuanciers hors-ligne complets** :
  - **DMC Mouliné** (`app/src/main/assets/dmc.json`) : 489 références
  - **Anchor** (`app/src/main/assets/anchor.json`) : 475 références
  - **Équivalences DMC ↔ Anchor** (`app/src/main/assets/equivalence.json`) : 591 paires
  - **Fils personnalisés** : ajout de marques ou références tierces avec choix de couleur
- **Inventaire visuel & gestion de stock** :
  - Tri par nuances chromatiques (teinte / saturation / luminosité) ou par code
  - Ajout rapide par lots de références (`BatchAddSheet`)
  - Suivi du nombre d'échevettes disponibles
- **Pipette photo (CIELAB ΔE)** :
  - Sélection d'une couleur sur une photo (moyenne 9×9 px) avec suggestion des 3 références DMC/Anchor les plus proches
- **Projets Bracelets** :
  - Création de modèles avec sélecteur visuel multi-couleurs (`MultiSelectThreadPickerDialog`)
  - Détection automatique des fils manquants par rapport à l'inventaire
  - Ajout des fils manquants à la liste de courses en un clic avec confirmation visuelle
- **Liste de courses** :
  - Compteur en temps réel dans la barre de navigation
  - Validation d'achat (passage en stock) ou suppression d'un fil de la liste
- **Sauvegarde & Mises à jour** :
  - Export / import complet de la base de données au format JSON (`BackupManager`)
  - Vérification et installation de mises à jour OTA depuis les **GitHub Releases** (`OtaUpdater`)

## Stack technique

- **Langage** : Kotlin `2.0.21` (JVM 17)
- **UI** : Jetpack Compose + Material 3 (Compose BOM `2024.10.01`)
- **Base de données** : Room `2.6.1` (KSP)
- **Sérialisation** : `kotlinx-serialization-json` `1.7.3`
- **SDK Android** : `minSdk 26` (Android 8.0) · `targetSdk 35` · `compileSdk 35`
- **Build** : Android Gradle Plugin `8.7.2` · Gradle `8.14.7`

## Architecture

```text
app/src/main/
├── assets/
│   ├── dmc.json
│   ├── anchor.json
│   └── equivalence.json
└── java/com/filotheque/app/
    ├── data/
    │   ├── backup/        # Export / import JSON
    │   ├── local/         # Base Room (AppDatabase)
    │   ├── repository/    # ThreadRepository, ProjectRepository
    │   └── update/        # OtaUpdater (GitHub Releases)
    ├── domain/
    │   └── color/         # ColorUtils (CIELAB ΔE, tri chromatique)
    └── ui/
        ├── components/    # Modales, sélecteurs, pipette photo, paramètres
        ├── screens/       # MainScreen, ThreadCollectionView, BraceletsScreen, ShoppingListScreen
        ├── theme/         # Thème Material 3
        └── viewmodel/     # ThreadViewModel, ProjectViewModel
```

## Build & CI/CD

### Build local

```bash
./gradlew test assembleRelease
```

*(ou `gradle test assembleRelease` si Gradle est installé localement)*

### GitHub Actions

Le workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) exécute automatiquement les tests et compile l'APK release sur chaque push sur `main`, et publie une GitHub Release contenant `filotheque.apk` sur chaque tag `v*`.

Secrets GitHub requis pour la signature release :
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
