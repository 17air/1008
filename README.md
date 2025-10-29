# Cardify

Cardify is a Kotlin Android sample that blends Google Maps, Jetpack Compose UI, a Room-powered local cache, and Firebase services to deliver an offline-friendly social gathering experience. Users can discover nearby groups on a map, manage their own meetups, and chat in real time with other participants.

## Features
- 🗺️ **Google Maps home screen** that launches by default, centers on the user's location, and shows every group as a marker.
- 📋 **Composable navigation flow** covering the map, group list, detail, creation, chat, and management screens.
- 🧭 **Floating action menu** for quick access to joined groups, creating a new group, or managing the groups you lead.
- 🗄️ **Room local database** seeded from bundled JSON (`seed_groups.json`, `recommended_tags.json`, `tag_embeddings.json`) to ensure sample meetups appear instantly offline.
- 🔄 **Realtime Firestore sync** that mirrors remote changes into the local cache and keeps chat messages live across devices.
- 🏷️ **Smart tag suggestions** that combine cosine-similarity embeddings with prefix matching so users can add relevant tags in a single tap.
- 💬 **Group chat** streams backed by Firestore snapshot listeners for immediate message delivery.

## Project Structure
```
app/
 ├─ src/main/java/com/example/cardify/
 │   ├─ CardifyApp.kt                # Application entry point, session + data bootstrap
 │   ├─ UserSession.kt               # Anonymous user info persisted in SharedPreferences
 │   ├─ ai/LocalTagRecommender.kt    # Offline tag embedding loader + recommendations
 │   ├─ data/
 │   │   ├─ GroupRepository.kt       # Room + Firestore coordinator and chat helpers
 │   │   ├─ local/…                  # Room database entities, DAO, converters
 │   │   └─ model/…                  # Group & ChatMessage models
 │   └─ ui/
 │       ├─ MainActivity.kt          # NavHost, FAB action sheet, top bar
 │       ├─ map/MapScreen.kt         # Google Map composable with markers & location
 │       ├─ list/GroupListScreen.kt  # Recommendation-aware group listing
 │       ├─ detail/GroupDetailScreen.kt
 │       ├─ create/GroupCreateScreen.kt
 │       ├─ chat/GroupChatScreen.kt
 │       ├─ joined/JoinedGroupsScreen.kt
 │       └─ mygroups/MyOwnedGroupsScreen.kt
 └─ src/main/assets/
     ├─ seed_groups.json             # Bundled meetups loaded into Room when empty
     ├─ recommended_tags.json        # Fallback suggestions for tag search
     └─ tag_embeddings.json          # 768-dimension tag vectors for similarity
```

## Google Maps & Firebase Setup
1. Enable the **Maps SDK for Android** in Google Cloud Console and replace the placeholder API key in `app/src/main/res/values/strings.xml` (`google_maps_key`).
2. Download your Firebase project's `google-services.json` and drop it into `app/`. The repo ships with a dummy config so the project compiles without secrets.
3. Enable **Cloud Firestore** and **Authentication** (anonymous sign-in is sufficient) for realtime data sync and chat.

## Running the App
1. Open the project in **Android Studio Koala (or newer)**.
2. Sync Gradle and let Android Studio download dependencies.
3. Build and run on a device or emulator that has Google Play Services.
4. Allow location access so the map centers on your current position; otherwise it falls back to the bundled Seoul coordinates.

## Offline Behaviour
- The first launch seeds the Room database using the JSON assets so sample meetups appear immediately, even without a network connection.
- If Firestore is unavailable, the app continues to operate on the local cache; chat messages fall back to in-memory streams until connectivity returns.

## License
This project is provided as-is for demo purposes.
