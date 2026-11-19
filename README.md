# LostLink — Smart Campus Lost & Found

LostLink is a native Android application written in Java with XML layouts, adapted from the supplied PHP project's workflows and UI. It includes registration/profile management, lost/found reports, search, claims, messaging, match suggestions, feedback, notifications, and local admin management.

## Project structure

```text
LostLink/
├── .gitignore
├── settings.gradle
├── build.gradle
├── gradle.properties
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/lostlink/app/
        │   ├── AdminReviewActivity.java
        │   ├── AdminConsoleActivity.java
        │   ├── AdminUsersActivity.java
        │   ├── AdminItemsActivity.java
        │   ├── AdminClaimsActivity.java
        │   ├── FeedbackActivity.java
        │   ├── SystemMessagesActivity.java
        │   ├── HelpActivity.java
        │   ├── SplashActivity.java
        │   ├── HomeActivity.java
        │   ├── LoginActivity.java
        │   ├── RegisterActivity.java
        │   ├── DashboardActivity.java
        │   ├── ReportItemActivity.java
        │   ├── ItemsActivity.java
        │   ├── ItemDetailsActivity.java
        │   ├── ProfileActivity.java
        │   ├── ClaimsActivity.java
        │   ├── MessagesActivity.java
        │   ├── NotificationsActivity.java
        │   ├── PotentialMatchesActivity.java
        │   └── data/ (SQLite, authentication, matching and notification classes)
        └── res/
            ├── drawable/
            ├── layout/
            └── values/
```

## Implemented features

- SQLite stores `Users`, `Items`, `Claims`, `Notifications`, `Messages`, and `Feedback`; first install seeds campus demo reports.
- Guests see a native landing page based on the supplied PHP site's hero, artwork, feature cards, and recent reports. They can search by item name and regional location before signing in.
- Registration collects username, mobile number, address, email, and a password that meets the PHP project's strength rules. Students can update their profile and change their password while signed in.
- Students can create, edit, search, filter, and delete their own reports. New or edited reports wait for admin review; owners can see their own reports while pending.
- Reports capture area, district, and taluka. Lost reports require ownership proof; found reports require finding evidence. A JPEG, PNG, or WEBP photo up to 2 MB is required; video remains optional.
- Approved reports are visible in public browsing and can be claimed. Owners can approve/decline claims, and either participant can mark an approved recovery resolved.
- The local administrator can manage student accounts, review reports, delete inappropriate items, decide claims with notes, read/reply to feedback, and send account messages.
- Photos and videos use Android's document picker without broad storage permission. Photos show in LostLink; video playback hands the selected clip to an installed video player.
- Possible matches compare approved lost/found reports by category, text, region, and a coarse grayscale image hash. This is a lightweight suggestion score, not object recognition or an automatic claim.
- Item-specific chat and in-app notifications are stored locally. Android device notifications are optional and require notification permission on Android 13+.
- About/contact, student feedback, and local account messages are available in the app. Email, dialer, map, and SMS compose actions use Android intents; LostLink does not send email/SMS directly or request call/SMS permissions.

## Demo accounts

- Student: `student@lostlink.edu` / `lostlink123`
- Admin: `admin@lostlink.edu` / `adminlink123`

Registration creates a local student account. The demo admin is seeded automatically. Its role is hard-coded and only suitable for testing on one device; production deployment needs server-side authentication and role checks. Email OTP/password-reset messages are not available offline: students can change their password while signed in, and the on-device admin can reset a student password.

## Build and run

Open this folder in Android Studio and allow Gradle sync. Use Android SDK Platform 35 and Android Build Tools 35.0.0, then run the `app` configuration on an emulator or device.

For a Windows PowerShell debug build using Android Studio's bundled Java runtime:

```powershell
$env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`. The wrapper is included; internet may be required to download Gradle or the Android Gradle Plugin on first use.

The debug build works with the installed Android Studio JBR. With Android Gradle Plugin 8.7.3, the optional `lintDebug` task currently fails under this machine's JBR 25.0.3 (`25.0.3` lint-worker error); use a JDK 17 Gradle runtime or update the Android Gradle Plugin/lint toolchain to run lint.

## Suggested workflow check

1. Register a student and edit the profile; test a signed-in password change.
2. Submit lost and found reports with their required evidence and photo. Confirm they remain pending and are hidden from public search.
3. Sign in as the admin to review the reports, then verify approved reports are searchable by item and region.
4. Create a claim and test both owner and admin claim decisions, including an admin note.
5. Test match suggestions, item chat, feedback submission/reply, admin/student messages, account management, and About/contact actions.

## Prototype limitations

All accounts, reports, claims, messages, feedback, and notifications are stored on a single device. There is no server endpoint/backend configured, so nothing synchronizes across devices. OTP email and server password recovery are unavailable offline. No fake API endpoint or credentials have been added. Server sync is a separate deployment phase that needs a real API base URL and agreed authentication/data contract. Local demo authentication is not suitable for real campus accounts.
