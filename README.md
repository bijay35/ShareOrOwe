# 💰 BillShare App

A personal Android app to track shared bills and IOUs between friends and family.

## Features

- **👥 People** — Add and manage people you share expenses with
- **🧾 Split Bill** — Record a bill, who paid, and split it equally among participants
- **🤝 IOU** — Track direct "I owe you" payments between two people
- **📊 Home (Balance Summary)** — See at a glance who owes whom and how much

## How to Build

### Prerequisites
- JDK 17+
- Android SDK (via command-line tools or Android Studio)
- Gradle (wrapper included)

### Build via terminal (Ubuntu)

```bash
# 1. Install JDK if not installed
sudo apt install openjdk-17-jdk

# 2. Download Android command-line tools from:
# https://developer.android.com/studio#command-tools
# Then set env variables in ~/.bashrc:
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# 3. Accept licenses
sdkmanager --licenses

# 4. Install required SDK
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 5. Build the APK
cd BillShareApp
chmod +x gradlew
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install on your phone

```bash
# Enable USB debugging on your Android phone
# Connect via USB then:
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK file to your phone and install manually (enable "Install from unknown sources" in settings).

## Git Setup

```bash
cd BillShareApp
git init
git add .
git commit -m "Initial commit: BillShare app"

# Push to GitHub
git remote add origin https://github.com/yourusername/BillShareApp.git
git push -u origin main
```

## App Usage

1. Go to **People** tab → Add names of everyone involved
2. For a shared dinner → Go to **Split** tab → Enter amount, who paid, select who's splitting
3. For a direct IOU → Go to **IOU** tab → Select who paid and who owes them back
4. Check **Home** tab anytime to see the net balance summary
   - both the Split and Owe pages now include filter controls at the top.  Use the person
     dropdown plus a status selector (All/Settled/Unsettled) and optional From/To dates
     to narrow results.

### Data export, import, and reports

- In **Settings** you can export your entire dataset to a JSON file. This is handy for backups or migrating
  between devices.
- Use "Import data from file" to restore a previously saved JSON; the app will restart after importing.
- In the **People** list, tap the share button next to a person to generate a simple plain-text transaction report
  containing only the transactions where both names appear. Split bills are listed first, then a section
  titled "Owe :" shows IOUs (description, amount and who paid).  A net balance calculation using actual
  names appears at the end.  You can send this via email, chat, etc.
