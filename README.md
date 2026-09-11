# AkinoClock

An analog clock in the style of the Braun BC12 alarm clock, sharing one screen with a
current-month calendar and a configurable RSS headline carousel. Built for a single target
tablet; not published to the Play Store.

## Build / test / install

```sh
./gradlew test                                  # all JVM unit tests (JUnit + Robolectric)
./gradlew assembleDebug                         # build the debug APK
./gradlew installDebug                          # install on the connected device
adb shell am start -n org.akinosoft.akinoclock.debug/.app.MainActivity
./gradlew connectedDebugAndroidTest             # instrumented tests (rare, on-device)
```
