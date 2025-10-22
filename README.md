# Cardify

Cardify is a Kotlin-based Android demo that combines Google Maps, Firebase Authentication, and Cloud Firestore to power a location-aware community experience. Users can discover nearby groups, inspect membership details, and collaborate in real time as participant counts stay synchronized across devices.

## Features
- 🔐 Automatic anonymous sign-in with Firebase Authentication
- 🗺️ Google Maps view that plots every Firestore group as a marker
- 📍 Distance-aware RecyclerView listing that mirrors the map
- 🧑‍🤝‍🧑 Real-time participant counts via Firestore snapshot listeners
- ✍️ Group creation workflow that stores metadata, coordinates, and owner membership
- ✅ Join/leave actions executed inside Firestore transactions with optimistic UI feedback

## Project Structure
```
app/
 ├─ src/main/java/com/example/cardify/
 │   ├─ CardifyApp.kt            # Firebase initialization & persistence
 │   ├─ UserSession.kt           # Anonymous auth helper
 │   ├─ data/
 │   │   ├─ FirestoreProvider.kt # Realtime listeners & transactions
 │   │   └─ model/
 │   │       ├─ Group.kt
 │   │       └─ Member.kt
 │   └─ ui/
 │       ├─ create/CreateGroupActivity.kt
 │       ├─ detail/GroupDetailActivity.kt
 │       └─ main/MainActivity.kt, GroupAdapter.kt
 └─ src/main/res/layout/…        # Material 3 view binding layouts
```

## Firebase Setup
1. Create a Firebase project and enable **Authentication → Anonymous sign-in** and **Firestore Database**.
2. Download your project's `google-services.json` file and place it in the `app/` directory.
3. (Optional) Deploy the provided [`firestore.rules`](firestore.rules) draft to enforce basic ownership checks.
4. Sync Gradle so the Google Services plugin can wire the configuration.

## Maps API
1. Enable the **Maps SDK for Android** in Google Cloud Console.
2. Update `app/src/main/res/values/strings.xml` with your Maps API key (`google_maps_key`).

## Running the App
1. Open the project in **Android Studio Hedgehog (or newer)**.
2. Sync Gradle and install dependencies.
3. Build and run on a Google Play Services-enabled device or emulator.

## License
This project is provided as-is for demo purposes.
