# Android Application

A modern Android application built with the latest technologies and best practices.



##Some Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/b37a14f4-3f33-4c0a-b025-f0267dfd7ff8" alt="Screenshot 1" width="30%" />
  <img src="https://github.com/user-attachments/assets/6f0b0616-20be-46ca-a464-5e3f36cf8ca2" alt="Screenshot 2" width="30%" />
  <img src="https://github.com/user-attachments/assets/a829bafd-cf67-4314-9b5c-0b3614d7fe15" alt="Screenshot 2" width="30%" />
</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/bf57ac14-04f5-45bd-b5bb-fb6e264a566e" alt="Screenshot 1" width="30%" />
  <img src="https://github.com/user-attachments/assets/454eb89d-5478-4e66-aaff-9acff09f80d5" alt="Screenshot 2" width="30%" />
  <img src="https://github.com/user-attachments/assets/9db6d775-8b9a-44dc-a997-4f541f91abfc" alt="Screenshot 2" width="30%" />
</p>


## Technologies Used

### Core Technologies

- **Kotlin** - Primary programming language
- **Android SDK** - Target SDK 35 (Android 15)
- **Gradle** - Build system with Kotlin DSL
- **AndroidX** - Modern Android support libraries

### Key Dependencies

- **AndroidX AppCompat** - Backward compatibility support
- **Material Design** - Material Design components
- **AndroidX Activity** - Activity and Fragment support
- **ConstraintLayout** - Flexible layout system
- **JUnit** - Unit testing framework
- **Espresso** - UI testing framework

## Features

- Modern Material Design UI
- Responsive layout using ConstraintLayout
- Backward compatibility support
- Secure API key management
- Release signing configuration
- Comprehensive test coverage

## Project Structure

```
app/
├── build.gradle.kts          # App-level build configuration
├── src/
│   └── main/
│       ├── java/            # Source code
│       ├── res/             # Resources
│       └── AndroidManifest.xml
├── release.keystore         # Release signing key
└── proguard-rules.pro       # ProGuard rules
```

## Setup Instructions

### Prerequisites

- Android Studio (Latest version)
- JDK 11 or higher
- Android SDK with API level 35
- Gradle 8.0 or higher

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project:
   - Debug build: `./gradlew assembleDebug`
   - Release build: `./gradlew assembleRelease`

### Release APK Generation

The project is configured with release signing. To generate a release APK:

1. Ensure you have the keystore file (`release.keystore`) in the app directory
2. Run `./gradlew assembleRelease`
3. Find the generated APK at: `app/build/outputs/apk/release/app-release.apk`

## Configuration

### API Key Management

The application uses a secure API key stored in the build configuration. The key is managed through the `buildConfigField` in `build.gradle.kts`.

### Signing Configuration

Release builds are signed using a keystore with the following configuration:

- Keystore file: `release.keystore`
- Key alias: `release`
- Validity: 10,000 days

## Testing

The project includes both unit tests and UI tests:

- Unit tests: `testImplementation(libs.junit)`
- UI tests: `androidTestImplementation(libs.espresso.core)`

Run tests using:

- Unit tests: `./gradlew test`
- UI tests: `./gradlew connectedAndroidTest`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For any queries or support, please contact the development team.
