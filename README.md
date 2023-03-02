# Acurast Processor

## [Acurast documentation](https://docs.acurast.com/acurast-transmitters)

<hr/>

## Development

### Setup development environment

1. Setup [Android Studio](https://developer.android.com/studio/install).

2. Clone repository

    ```sh
    git clone https://github.com/Acurast/data-processor.git
    ```

3. Open `data-processor` with Android Studio.

<hr/>

### Run unit tests

```sh
./gradlew clean test_testnetReleaseUnitTest

# Test results can be found at: app/build/reports/tests/test_testnetReleaseUnitTest/index.html
```

### Run instrumented android tests

This step requires an android virtual device. [Start the emulator from the command line](https://developer.android.com/studio/run/emulator-commandline)

```sh
./gradlew connected_testnetDebugAndroidTest

# Test results can be found at: app/build/reports/androidTests/connected/flavors/_testnet/index.html
```

<hr/>

### Build Android APK

```sh
./gradlew assemble
```
