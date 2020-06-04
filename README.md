StopCovid Android
=================

This repository is a fork of [upstream gitlab.inria.fr][upstream]
repo `stopcovid-android`, the Android mobile application component
of [StopCovid][StopCovid].


Building the app
----------------

0. install `gradle`
1. install `android-sdk` for your dev env
2. make sure the sdk dir user-writable (gradle might try to update it)
3. tell gradle where the sdk is

	cp local.properties.sample local.properties  # (and set sdk.dir)

4. build the app (default = debug, for gradle 1.3.71)

	gradle assemble

5. output: `./stopcovid/build/outputs/apk/debug/stopcovid-debug.apk`


Building the release (signed) apk
---------------------------------

1. create local keystore in stopcovid/

	keytool -genkey -v -keystore my-release-key.jks \
		-keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
	mv my-release-key.jks stopcovid/

2. build the release apk

	gradle assembleRelease

3. output: `./stopcovid/build/outputs/apk/release/stopcovid-release.apk`


Installing the app
------------------

	gradle installRelease


Checking the log live with adb
------------------------------

	adb logcat | grep `adb shell ps | grep fr.gouv  | awk '{print $2}'`



[StopCovid]: https://gitlab.inria.fr/stopcovid19/accueil/
[upstream]:  https://gitlab.inria.fr/stopcovid19/stopcovid-android/
