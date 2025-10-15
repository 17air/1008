# Cardify

Cardify is a simple Android demo application written in Kotlin that showcases Google Maps integration and location services. The app lets users browse nearby hobby groups on top of a map centered around their current position and register a new group at their location.

## Features
- Google Maps view highlighting dummy groups near Seoul
- Current location tracking via `FusedLocationProviderClient`
- Runtime location-permission handling
- RecyclerView list of nearby groups
- Group creation form that reverse-geocodes the current location before submitting
- Optional Firestore integration for loading real group markers when Firebase is configured

## Getting Started
1. Open the project in Android Studio Hedgehog or newer.
2. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` in `app/src/main/AndroidManifest.xml` with a valid Google Maps API key.
3. (Optional) If you have a Firebase project, place your `google-services.json` file in the `app/` directory to enable Firestore-backed groups.
4. Sync the Gradle project and run the app on a device or emulator with Google Play Services.

## License
This project is provided as-is for demo purposes.
