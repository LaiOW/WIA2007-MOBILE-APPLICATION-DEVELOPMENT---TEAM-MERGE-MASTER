# MergeAid

MergeAid is an Android emergency support app designed to help users report SOS situations, view emergency cases on a live map, and stay connected through a community feed.

The app combines location tracking, real-time SOS monitoring, user accounts, profile management, favorites, and a social-style post feed to support emergency response workflows.

> Important: MergeAid is a support tool, not a replacement for official emergency services. If someone is in immediate danger, contact local emergency services first.

## Overview

MergeAid is built around two main experiences:

1. A map-based emergency dashboard that displays SOS calls, nearby incidents, route guidance, and live alerts.
2. A community feed where users can create posts, browse content, manage a profile, and save favorite locations for quick access.

The app uses Supabase for authentication, database storage, realtime updates, and image storage. It also uses OpenStreetMap via osmdroid for the map interface.

## Features

- Email and password authentication with Supabase
- Splash, landing, login, and registration flow
- Emergency map with the user's current location
- SOS call reporting with stored coordinates and timestamp
- Live SOS marker updates from Supabase Realtime
- Nearby SOS notification service running in the foreground
- Route drawing from the user to an SOS location
- Search and autocomplete for places on the map
- Favorites list for quick location access
- Community feed for creating and viewing posts
- Image upload support for post attachments and profile photos
- Profile page with display name, bio, certificates, and badges
- Trending and category post screens

## Tech Stack

- Android Studio
- Java
- Kotlin
- XML layouts
- Supabase Auth
- Supabase PostgREST
- Supabase Storage
- Supabase Realtime
- osmdroid / OpenStreetMap
- Picasso for image loading
- Material Components
- Navigation Component

## Project Structure

- `app/src/main/java/com/example/myapplication/MainActivity.java` - app entry point after login, starts SOS monitoring
- `app/src/main/java/com/example/myapplication/home_page.java` - main home screen with ViewPager tabs
- `app/src/main/java/com/example/myapplication/fragment_map.java` - emergency map, SOS markers, routes, search, and alerts
- `app/src/main/java/com/example/myapplication/HomeFragment.java` - community feed
- `app/src/main/java/com/example/myapplication/FavoritesFragment.java` - saved favorite locations
- `app/src/main/java/com/example/myapplication/TrendingFragment.java` - trending content view
- `app/src/main/java/com/example/myapplication/ProfileActivity.java` - profile editing, badges, and logout
- `app/src/main/java/com/example/myapplication/CreatePostActivity.java` - create a new community post
- `app/src/main/java/com/example/myapplication/SOSNotificationService.java` - foreground service for nearby SOS notifications
- `app/src/main/java/com/example/myapplication/SupabaseManager.kt` - Supabase auth, database, storage, and realtime access

## Requirements

- Android Studio Hedgehog or newer is recommended
- JDK 17
- Android SDK with compile target 36
- Minimum Android version: API 29
- A Supabase project with Auth, Postgres, Storage, and Realtime enabled

## Permissions Used

The app requests these Android permissions:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION`
- `POST_NOTIFICATIONS`
- `WAKE_LOCK`

These permissions are used for map access, SOS tracking, realtime alerts, and the foreground monitoring service.

## Backend Setup

MergeAid expects a Supabase backend. The code currently uses the following resources:

- `posts` table for community posts
- `SOS calls` table for emergency reports
- `Posts` storage bucket for post and profile images

### Suggested `posts` table fields

The app decodes posts into the `Post` model, which includes:

- `id`
- `userName`
- `title`
- `content`
- `category`
- `imageUri`
- `user_profile_image`
- `likeCount`
- `isLiked`

### Suggested `SOS calls` table fields

The app expects SOS records with:

- `id`
- `time`
- `username`
- `x_coordinate`
- `y_coordinate`

### Realtime

The app subscribes to inserts on the `SOS calls` table, so realtime updates should be enabled for that table.

## How the App Works

### 1. Launch flow

- `SplashActivity` launches first.
- `LandingActivity` checks whether GPS is enabled.
- If a Supabase session exists, the app opens the main home screen automatically.
- If no session exists, the app routes the user to login and registration.

### 2. Emergency map flow

- The map shows the user's current location.
- SOS calls are loaded from Supabase and shown as markers.
- The app highlights nearby SOS calls and can show routes to selected incidents.
- Double-tapping an SOS marker reveals the option to accept the case.
- Accepting a case removes it from the backend and refreshes the map stats.

### 3. SOS reporting flow

- The user presses the SOS call button.
- The app reads the current location.
- The location is stored in Supabase as a new SOS call.
- Nearby users receive a notification through the foreground service and realtime subscription.

### 4. Community feed flow

- Users can create a post with optional image upload.
- Posts are stored in Supabase and displayed in the feed.
- Profile images are reused in post creation and feed display.

### 5. Favorites flow

- Favorite locations are stored locally in shared preferences.
- Selecting a favorite returns the map to that location search.

## Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Ensure your device or emulator has location services enabled.
5. Build and run the app on an Android device or emulator.

## Running the App

### From Android Studio

1. Open the project.
2. Wait for Gradle sync to complete.
3. Select a device or emulator.
4. Click Run.

### From the command line

If your environment is configured for Gradle, you can use the standard Android Gradle wrapper:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
gradlew.bat assembleDebug
```

## Default App Behavior

- The app starts from `SplashActivity`.
- Logged-in users are redirected to `home_page`.
- Logged-out users see the landing/login flow.
- The main map and SOS monitoring require location permissions.

## Notes for Contributors

- Some screens contain scaffolded or sample data, such as the trending list.
- The map and SOS features depend on a working Supabase backend and location services.
- The app uses both Java and Kotlin sources, so keep interoperability in mind when adding new models or callbacks.
- Avoid hard-coding sensitive backend credentials in new code.

## Troubleshooting

- If the map is blank, check location permissions and GPS availability.
- If SOS markers do not appear, verify the Supabase table name, schema, and realtime configuration.
- If image uploads fail, confirm that the `Posts` bucket exists and storage permissions are correct.
- If login fails, confirm that Supabase Auth is enabled and the project credentials are valid.

## License

This project is distributed under the terms of the repository license.

