# Cardify

Cardify is a Kotlin-based Android demo that combines Google Maps and a lightweight in-memory data layer to power a location-aware community experience. Users can discover nearby groups, inspect membership details, and manage participation entirely offline without any backend services.

## Features
- 🗺️ Google Maps view that plots every locally stored group as a marker
- 📍 Distance-aware RecyclerView listing that mirrors the map
- 🧑‍🤝‍🧑 Real-time participant counts powered by an observable in-memory repository
- ✍️ Group creation workflow that stores metadata, coordinates, and owner membership
- ✅ Join/leave actions executed atomically inside the local repository with instant feedback
- 🌱 Preloaded Seoul-area sample groups so the experience works immediately offline

## Project Structure
```
app/
 ├─ src/main/java/com/example/cardify/
 │   ├─ CardifyApp.kt            # App-wide initialization for session & data store
 │   ├─ UserSession.kt           # Anonymous local user helper
 │   ├─ data/
 │   │   ├─ LocalGroupRepository.kt # In-memory realtime store & transactions
 │   │   └─ model/
 │   │       ├─ Group.kt
 │   │       └─ Member.kt
 │   └─ ui/
 │       ├─ create/CreateGroupActivity.kt
 │       ├─ detail/GroupDetailActivity.kt
 │       └─ main/MainActivity.kt, GroupAdapter.kt
 └─ src/main/res/layout/…        # Material 3 view binding layouts
```

## Maps API
1. Enable the **Maps SDK for Android** in Google Cloud Console.
2. Update `app/src/main/res/values/strings.xml` with your Maps API key (`google_maps_key`).

## Running the App
1. Open the project in **Android Studio Hedgehog (or newer)**.
2. Sync Gradle and install dependencies.
3. Build and run on a Google Play Services-enabled device or emulator.
4. All group information lives in memory, so restarting the app resets it to the bundled sample data.

## License
This project is provided as-is for demo purposes.
