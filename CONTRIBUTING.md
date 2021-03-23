Contributing
============

Use the app !
-------------

1. install the [latest release apk][release] on your phone
2. play with the app
3. suggest improvements or report issue in [the issues tracker][tracker]


[release]: ../../releases/latest/download/stopcovid-release.apk
[tracker]: ../../issues


Fixing things
-------------

0. setup a dev environment
1. hack the code
2. test it works
3. create a pull-request


Building the app
----------------

1. install `android-sdk` for your dev env
2. make sure the sdk dir is user-writable (gradle might try to update it)
3. tell gradle where the sdk is
   ```
	cp local.properties.sample local.properties  # (and set sdk.dir)
   ```
4. build the app
   ```
	make build
   ```
5. output: `./stopcovid/build/outputs/apk/release/stopcovid-release.apk`


Building the debug apk
----------------------

1. build the (debug) app
   ```
	./gradlew assembleDebug
   ```
2. output: `./stopcovid/build/outputs/apk/debug/stopcovid-debug.apk`


Installing the app
------------------

1. switch your phone to `developer mode`
   ```
	Parameters > About this phone > build number: tap 7 times
	Parameters > System > Advanced > Developers > USB Debugging: enable
   ```
2. install the app
   ```
	make install
   ```

Checking the log live with adb
------------------------------

   ```
	make log
   ```
