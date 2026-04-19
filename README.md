# Pro Note

A simple Android note application with online/offline sync support, built in Java.

## Overview

`Pro Note` lets users create and manage notes with offline persistence and Firebase-backed synchronization. Offline notes are cached locally using Room and automatically synced to Firestore when network connectivity is available.

## Key Features

- Create, edit, and delete notes
- Offline note storage with Room
- Firestore sync for online notes
- Pending sync indicator for offline notes
- Firebase Authentication integration
- Background sync using WorkManager
- Support for note attachments (images, URLs, pages, text)

## Tech Stack

- Android SDK
- Java
- AndroidX AppCompat
- Material Components
- Room Database
- WorkManager
- Firebase Authentication
- Firebase Firestore
- Glide
- Gson

## Requirements

- Android Studio
- Android SDK API Level 35
- Java 17
- `google-services.json` configured in `app/` (keep this file local only)

## Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Place your Firebase `google-services.json` file inside `app/`.
4. Sync Gradle.

## Build

From the project root:

```bash
./gradlew.bat assembleDebug
```

## Run

- Launch the app from Android Studio on an emulator or physical device.
- Sign in with Firebase auth (if configured).
- Add notes and verify offline sync behavior.

## Important Files

- `app/src/main/java/com/mtech/note/MainActivity.java` — main notes list UI
- `app/src/main/java/com/mtech/note/NoteDetailsActivity.java` — add/edit note screen
- `app/src/main/java/com/mtech/note/offline/AppDatabase.java` — Room database setup
- `app/src/main/java/com/mtech/note/offline/LocalNoteDao.java` — DAO for offline notes
- `app/src/main/java/com/mtech/note/sync/NoteSyncWorker.java` — background sync worker
- `app/src/main/java/com/mtech/note/sync/SyncScheduler.java` — enqueue sync jobs

## Notes

- Firebase must be properly configured for authentication and Firestore.
- The app uses a local Room database file named `pro_note.db`.
- `BuildToolsVersion` is set to `35.0.0` in `app/build.gradle`.

## License

This repository does not include a specific license file.
