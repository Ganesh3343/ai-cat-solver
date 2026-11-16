AI CAT SOLVER — ANDROID PROJECT

This is an Android Studio project source, not a compiled APK. This workspace does not have the Android SDK/Gradle build environment needed to compile and sign an APK.

Before building:
1. Deploy the cloud backend from the earlier AI_CAT_SOLVER_source.zip project.
2. Copy the deployed backend URL into app/build.gradle.kts:
   buildConfigField("String", "API_BASE_URL", "\"https://YOUR-REAL-SERVICE-URL/\"")
   Keep the trailing slash.
3. Open this folder in Android Studio (current stable version).
4. Let Gradle sync and install Android SDK Platform 35 if prompted.
5. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
6. Android Studio creates the debug APK at:
   app/build/outputs/apk/debug/app-debug.apk

For a distributable signed APK, use Build > Generate Signed Bundle / APK.

IMPORTANT:
- Do not put your OpenAI API key in the Android app. It belongs only in the cloud backend's server environment.
- Cloud backend must be deployed and reachable before the app can solve images.
- API usage may incur OpenAI charges.
