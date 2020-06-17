StopCovid Android
=================

This _extended_ StopCovid features:

- notifications on "ping"
- proximity info screen
- ui beautifications

The app is a fork of upstream [gitlab.inria.fr][upstream]
`stopcovid-android` and is compatible with [StopCovid][StopCovid].

[![download latest][download]][release]


Building the app
----------------

0. install `gradle`
1. install `android-sdk` for your dev env
2. make sure the sdk dir is user-writable (gradle might try to update it)
3. tell gradle where the sdk is
   ```
	cp local.properties.sample local.properties  # (and set sdk.dir)
   ```
4. build the (debug) app
   ```
	gradle assembleDebug
   ```
5. output: `./stopcovid/build/outputs/apk/debug/stopcovid-debug.apk`


note: alternatively, skip step 0. above and use `./gradlew assembleDebug`


Building the release (signed) apk
---------------------------------

1. create local keystore in stopcovid/ (use _password_ as password)
   ```
	keytool -genkey -v -keystore my-release-key.jks \
		-keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
	mv my-release-key.jks stopcovid/
   ```
2. build the release apk
   ```
	gradle assembleRelease
   ```
3. output: `./stopcovid/build/outputs/apk/release/stopcovid-release.apk`


Installing the app
------------------

1. switch your phone to `developer mode`
   ```
	Parameters > About this phone > build number: tap 7 times
	Parameters > System > Advanced > Developers > USB Debugging: enable
   ```
2. install the app
   ```
	gradle installRelease
   ```

Checking the log live with adb
------------------------------

	adb logcat | grep `adb shell ps | grep fr.gouv  | awk '{print $2}'`



[StopCovid]: https://gitlab.inria.fr/stopcovid19/accueil/
[upstream]:  https://gitlab.inria.fr/stopcovid19/stopcovid-android/
[download]:  https://yt3dl.net/images/apk-download-badge.png
[release]:   ../../releases/latest/download/stopcovid-release.apk
