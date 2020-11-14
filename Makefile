all:

APP = fr.gouv.android.stopcovid
ADB = adb shell run-as $(APP)
FILES = /data/data/$(APP)/files

ls:
	$(ADB) ls -lh $(FILES)

rm:
	$(ADB) rm $(FILES)/localProximity.txt

cp:
	$(ADB) cat $(FILES)/localProximity.txt		> data.txt
	$(ADB) cat $(FILES)/disseminatedEbids.txt	> ebids.txt

cat.%:
	@$(ADB) cat $(FILES)/$*

load: data.load.txt
	$(MAKE) cp rm --ignore-errors --quiet
	$(ADB) tee -a $(FILES)/localProximity.txt	< data.load.txt \
							> /dev/null

tar:
	$(ADB) tar c $(FILES)/local_proximity		> data.tar


keystore = stopcovid/my-release-key.jks

build: $(keystore) .
	./gradlew assembleRelease

$(keystore):
	echo "password,password,,,,,,,yes" | tr , "\n" |\
	keytool -genkey -v -keystore $(keystore) \
		-keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
