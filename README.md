# Cardify

Cardify is an Android sample built entirely in Kotlin that showcases how to combine Google Maps, Jetpack Compose, and a fully local Room database to run a location-centric group discovery experience without any server dependencies. The project is designed to feel realistic out of the box: twenty seeded meetups appear on the map the first time you launch the app, smart tag recommendations react to your description text, and every screen stays available offline after the first run.

## Highlights
- 🗺️ **Map-first launch** – the app starts on the Google Map, keeps the blue location dot enabled, and hides non-essential chrome (compass, toolbar) so users focus on nearby groups.
- 📋 **Composable navigation flow** – a single `NavHost` drives the map, full list, detail, joined, owned, and creation screens. The two "전체 그룹" buttons on the map surface open the full catalog or the additional options sheet.
- 🏡 **Room-backed offline data** – groups and memberships are stored locally. When the database is empty it loads bundled JSON (`seed_groups.json`) that already contains 20 Seoul-area meetups so the UI never looks empty.
- 🏷️ **Smart tag engine** – `LocalTagRecommender` loads 768-dimension embeddings from `tag_embeddings.json` and mixes cosine similarity with prefix matches from `recommended_tags.json` to surface 1–3 relevant tags as you type.
- 📍 **Location-aware list sorting** – the "전체 그룹" screen highlights tag matches and groups within five kilometres above the full catalog, each rendered as an underlined quick link that refocuses the map.

## Project Layout
```
app/
 ├─ src/main/java/com/example/cardify/
 │   ├─ CardifyApp.kt            # Application class – seeds Room, initialises user session
 │   ├─ UserSession.kt           # SharedPreferences-backed user name/tag helpers
 │   ├─ ai/LocalTagRecommender.kt
 │   ├─ data/
 │   │   ├─ GroupRepository.kt   # Repository exposing flows + creation/join logic
 │   │   ├─ local/               # Room database, DAO, entities, converters
 │   │   └─ model/Group.kt       # Public model consumed by UI
 │   └─ ui/
 │       ├─ MainActivity.kt      # Compose entry, NavHost, modal sheet wiring
 │       ├─ map/MapScreen.kt     # GoogleMap composable with marker rendering
 │       ├─ list/GroupListScreen.kt
 │       ├─ detail/GroupDetailScreen.kt
 │       ├─ create/GroupCreateScreen.kt
 │       ├─ joined/JoinedGroupsScreen.kt
 │       └─ mygroups/MyOwnedGroupsScreen.kt
 └─ src/main/assets/
     ├─ seed_groups.json         # 20 starter groups with coordinates & tags
     ├─ recommended_tags.json    # Simple fallback tag list
     └─ tag_embeddings.json      # 768-float embedding vectors per tag
```

## Screen Flow
1. **MapScreen** (start destination)
   - Requests fine location permission.
   - Recenters on the user when GPS succeeds, otherwise shows the Seoul fallback coordinate.
   - Plots every group marker; tapping a marker opens the detail screen.
   - Shows a bottom card with two buttons: "전체 그룹(n)" navigates to the list, "추가 메뉴 열기" opens the modal sheet for joined/creation/owned shortcuts.
2. **GroupListScreen** (전체 그룹)
   - Displays two recommendation blocks at the top:
     - `내 태그(XX)와 유사한 추천 그룹(Y개)` – hyperlinks groups whose tags match the user’s tag.
     - `반경 5km 이내 그룹(Z개)` – hyperlinks groups within 5 km of the saved user location.
   - Clicking any hyperlink pops back to the map and focuses that group.
   - Below the recommendations, the full catalog is sorted by similarity to tags from groups you have joined.
3. **GroupDetailScreen** – read-only summary with join button (toggles to "참여중" when already a member) and leader badge.
4. **GroupCreateScreen** – single-line inputs for title, schedule, description, and tags. Recommended tag chips appear between description and tags and append to the comma-separated list with one tap. A compact map lets you long-press to set the meetup location.
5. **JoinedGroupsScreen / MyOwnedGroupsScreen** – scrollable cards that reflect the user’s memberships or groups they own. Owned groups expose inline edit/delete actions.

## Data & Persistence
- **Room schema** – `GroupEntity` and `MembershipEntity` capture group metadata and user enrolment. `GroupDao` exposes suspend functions and cold flows that power the UI.
- **Seeding** – `GroupRepository` copies the bundled assets into Room on first launch. The JSON includes lat/lng coordinates so markers appear immediately.
- **Mutations** – Creating, joining, editing, and deleting groups all operate locally and instantly update the Compose flows.

## Tag Recommendation Details
- The recommender loads embeddings from `tag_embeddings.json` (or generates deterministic dummy vectors if the asset is missing).
- Input descriptions are embedded via a lightweight character-level encoder.
- Cosine similarity above `0.25` is treated as meaningful; the engine returns between 1 and 3 tags. If nothing meets the threshold it falls back to prefix matches or the curated list.
- Chips are rendered with `AssistChip` – tapping a chip appends the tag to the comma-separated field without duplicates.

## Location Behaviour
- Map UI settings keep only the blue dot and the GPS button visible; compass and toolbar are disabled.
- The current location is hoisted to `MainActivity` so both the map and the list can react to the same coordinate.
- The "5km" recommendation block uses `Location.distanceBetween` for accurate great-circle distance checks.

## Getting Started
1. **Prerequisites**
   - Android Studio Koala or newer
   - Android SDK 34, Google Play Services
2. **Setup**
   - Clone this repository.
   - Open the project in Android Studio and let Gradle sync.
   - Provide a Google Maps API key if you need live map tiles (the sample ships with a placeholder).
3. **Run**
   - Build & deploy to a device/emulator with Google Play Services.
   - Grant location permission when prompted to unlock the blue dot and nearby-group filtering.

## Troubleshooting
- **No markers?** Ensure the seeding JSON copied correctly by clearing app data or uninstalling and reinstalling.
- **No recommended tags?** Provide at least a few descriptive words; the recommender trims whitespace and only returns tags above the similarity threshold.
- **Gradle memory issues?** Increase the daemon heap in `gradle.properties` as documented (the project already requests 2G for dexing Room + Compose artefacts).

## License
This repository is provided for educational use. Feel free to adapt the patterns for your own offline-first prototypes.
