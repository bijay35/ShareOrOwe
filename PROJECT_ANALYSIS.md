# BillShareApp — Project Analysis

## Overview

**BillShareApp** is a native Android application written in Kotlin for tracking shared bills and IOUs ("I owe you") between friends and family. It is a personal/offline-first app: all data is persisted locally via `SharedPreferences` (no backend, no network). Backups are handled through manual JSON export/import.

- **Language:** Kotlin
- **Build system:** Gradle (wrapper included)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK / Compile SDK:** 34 (Android 14)
- **Application ID:** `com.billshare.app`
- **JVM target:** 1.8

## Architecture

The app follows a single-activity + multiple-fragments pattern with a flat utility layer. There is no MVVM/Repository abstraction — fragments call the `DataManager` singleton directly.

```
LoginActivity (first-run / re-login)
        │
        ▼
MainActivity ──► NavHostFragment
        │
        └─► BottomNavigationView
                ├── Home (balance summary, split/owe sub-tabs)
                ├── Transaction (split bill / IOU entry)
                ├── People (manage people, per-person reports)
                └── Settings (export / import / clear / logout)
```

### Module layout

```
app/src/main/java/com/billshare/app/
├── ui/
│   ├── MainActivity.kt          # Hosts the NavController + BottomNav
│   └── LoginActivity.kt         # First-run user creation / login
├── fragments/                   # 11 fragments (screens)
│   ├── HomeFragment.kt          # Container with split/owe sub-tabs
│   ├── HomeSplitFragment.kt     # Split-bill balance view (385 LOC)
│   ├── HomeOweFragment.kt       # IOU balance view (289 LOC)
│   ├── TransactionsFragment.kt  # Entry point for new transactions
│   ├── SplitBillFragment.kt     # Create / edit split bills
│   ├── IOUFragment.kt           # Create / edit IOUs
│   ├── BillDetailsFragment.kt
│   ├── IOUDetailsFragment.kt
│   ├── PeopleFragment.kt
│   ├── PersonDetailsFragment.kt
│   └── SettingsFragment.kt
├── adapters/                    # RecyclerView adapters
│   ├── BalanceAdapter.kt
│   ├── IOUAdapter.kt
│   ├── PersonAdapter.kt
│   ├── PersonBillAdapter.kt
│   └── SplitSummaryAdapter.kt
├── models/
│   └── Models.kt                # All data classes in a single file
└── utils/
    └── DataManager.kt           # Persistence + report generation
```

## Domain Model (`models/Models.kt`)

| Type | Purpose | Key fields |
|---|---|---|
| `Person` | A participant (`@Parcelize`) | `id` (UUID), `name` |
| `SplitBill` | A bill split among N participants | `description`, `paidBy`, `totalAmount`, `participants`, `date`, `isSettled`; computed `sharePerPerson` and `owedByOthers` |
| `IOU` | A direct one-to-one debt | `description`, `paidBy`, `owedTo`, `amount`, `date`, `isSettled` |
| `Settlement` | Records that one participant has paid back their share of a specific bill | `billId`, `personId`, `settledAmount`, `date` |
| `Balance` | View-only net amount per person | `person`, `netAmount` (positive = is owed, negative = owes) |

Settlements are **per-participant per-bill**, which lets a multi-person bill be partially settled — `DataManager.isBillFullySettled` returns true only when every non-payer has a `Settlement` row for the bill.

## Persistence (`utils/DataManager.kt`)

A Kotlin `object` (singleton) that serializes everything to a single `SharedPreferences` file (`BillSharePrefs`) using **Gson**.

Storage keys:
- `persons`, `split_bills`, `ious`, `settlements`, `current_user`

Key responsibilities:
- CRUD for persons / split bills / IOUs / settlements
- "Current user" handling (drives the `LoginActivity` redirect in `MainActivity.onCreate`)
- `exportAllData(context)` → JSON wrapper (`ExportWrapper`) of everything including current user
- `importAllData(context, json)` → clears and replaces all data; reattaches `currentUser` by id, falling back to case-insensitive name match
- `getFormattedReportForUser(...)` → human-readable plain-text report between the current user and another person, grouping splits by participant count and computing a net balance

## Navigation & UI

- **Navigation Component** with a single `nav_graph.xml` and 4 bottom-nav destinations: Home, Transaction, People, Settings (`res/menu/bottom_nav_menu.xml`).
- **View Binding** is enabled (`buildFeatures.viewBinding true`); generated bindings exist for every layout.
- 18 layouts under `res/layout/` (one per activity/fragment plus `item_*` row layouts for adapters).

## Dependencies (`app/build.gradle`)

| Library | Version | Use |
|---|---|---|
| `androidx.core:core-ktx` | 1.12.0 | Kotlin extensions |
| `androidx.appcompat:appcompat` | 1.6.1 | Compat Activity |
| `com.google.android.material:material` | 1.11.0 | Material components, BottomNav |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Layouts |
| `androidx.recyclerview:recyclerview` | 1.3.2 | Lists |
| `androidx.cardview:cardview` | 1.0.0 | Cards |
| `androidx.navigation:navigation-fragment-ktx` | 2.7.6 | Single-activity nav |
| `androidx.navigation:navigation-ui-ktx` | 2.7.6 | BottomNav ↔ NavController |
| `com.google.code.gson:gson` | 2.10.1 | JSON for prefs + export/import |

Plugins: `com.android.application`, `org.jetbrains.kotlin.android`, `kotlin-parcelize`.

## Features (from code + README)

1. **People management** — add/remove/rename people; current user is required and chosen at login.
2. **Split bills** — record description, payer, total, and a list of participants. Equal split derived as `total / participants.size`. Filterable home view (person, status, date range).
3. **IOUs** — direct payer→ower records with amount and description.
4. **Per-bill settlement** — each non-payer participant can be marked settled individually; the bill flips to fully settled once all of them are.
5. **Home balance summary** — net `who owes whom` view across both splits and IOUs, split into Split and Owe sub-tabs with collapsible filters.
6. **Per-person plain-text report** — share a per-counterparty summary via the OS share sheet (email, chat, etc.).
7. **JSON backup/restore** — export and import the full dataset from Settings; the app restarts after import.

## Build & Run

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17+ and Android SDK platform 34 / build-tools 34.0.0. See `README.md` for the full Ubuntu setup.

## Notable Observations / Risks

- **No tests.** There is no `app/src/test` or `app/src/androidTest` source set. The `test_calculation.txt` and `test_new_logic.txt` at the repo root appear to be scratch notes, not executable tests.
- **`getFormattedReportForUser` contains hard-coded variable names** (`bijayOwesKamalFromSplits`, `kamalOwesBijayFromSplits`, etc.). They function correctly because they are just locals, but they read like leftover debugging names from the original author's data and should be renamed to `meOwesPersonFromSplits` / `personOwesMeFromSplits`.
- **Branch in `getFormattedReportForUser` is unreachable.** The `bill.participants.size > 2` `when` arm duplicates the two arms above it, so it can never match. Safe to delete.
- **Single-file model and singleton persistence** keep things simple but mean any schema change requires manual JSON migration logic in `importAllData` (currently none — older exports without `settlements` would deserialize with `null` and crash on the non-null `List` field).
- **`release` build is unminified** (`minifyEnabled false`) and there is no signing config, so the project as-is only produces debug APKs.
- **No string externalization for many UI labels** — bottom-nav titles ("Home", "Transaction", "People", "Settings") are inline in `bottom_nav_menu.xml` rather than `@string/...`, which blocks localization.
- **`allowBackup="true"`** in the manifest combined with unencrypted `SharedPreferences` means the dataset can be pulled via ADB backup on a debuggable build — fine for a personal app, worth noting if it ever ships to the Play Store.

## File Inventory

- Kotlin source: 17 files, ~1,810 LOC (largest: `HomeSplitFragment.kt` 385, `DataManager.kt` 329, `HomeOweFragment.kt` 289).
- Layouts: 18 XML files.
- Resources: 5 mipmap densities, single nav graph, single bottom-nav menu.
- Helper directories `fix_patch/` and `icons_patch/` at the repo root look like one-off patch staging from the project's history.
