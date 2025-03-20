# Timestamp Editor

**Timestamp Editor** is an Android app that allows users to batch edit file timestamps (e.g., "Last Modified" date) directly on their device.

## ✨ Features

- 🗂️ Batch edit timestamps for multiple files.
- 🕒 Change the "Last Modified" and "Created" date to any custom date/time.
- ⚙️ Automatic fallback to **root mode** for files that require elevated permissions.
- 📂 Works with both internal and external storage file systems.

## ⚠️ Root Access

- The app will first attempt to update file timestamps using normal Android APIs.
- If this fails (e.g., due to Android storage restrictions), the app will prompt to use **root access** (if enabled in settings).
- Root is recommended for full compatibility with restricted files or system directories.

## 📸 App Screenshots

![Alt text](https://raw.githubusercontent.com/grmasa/Timestamp-Editor/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png?raw=true "Screenshot 1")
![Alt text](https://raw.githubusercontent.com/grmasa/Timestamp-Editor/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png?raw=true "Screenshot 2")

## 🚀 Usage

1. Select the files you want to modify.
2. Pick a custom date and time.
3. Tap "Apply" to update the timestamps.
4. Optionally, enable root mode in settings for protected files.

## 📱 Requirements

- Android 5.0+ (Lollipop or higher)
- Root access (optional, but might be required on modern devices)

## 📝 License

This project is licensed under GPL-3.0-or-later - see the [LICENSE](LICENSE) file for details
