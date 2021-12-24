all:

APP = fr.gouv.android.stopcovid
ADB = adb shell run-as $(APP)
FILES = /data/data/$(APP)/files

ls:
	$(ADB) ls -lh $(FILES)

rm:
	$(ADB) rm $(FILES)/localProximity.txt

save:
	$(ADB) cat $(FILES)/localProximity.txt		> data.txt
	$(ADB) cat $(FILES)/disseminatedEbids.txt	> ebids.txt

restore:
	$(ADB) tee $(FILES)/localProximity.txt		< data.txt
	$(ADB) tee $(FILES)/disseminatedEbids.txt	< ebids.txt

cat.%:
	@$(ADB) cat $(FILES)/$*

load: data.load.txt
	$(MAKE) save rm --ignore-errors --quiet
	$(ADB) tee -a $(FILES)/localProximity.txt	< data.load.txt \
							> /dev/null
log:
	adb logcat | grep `adb shell ps | grep fr.gouv  | awk '{print $$2}'`

tar:
	$(ADB) tar c $(FILES)/local_proximity		> data.tar

data.log:
	bash -c 'p() { date +"%a %F %T" -d@$$((36#$$1 - 2208988800)); \
		cut -d " " -f 2- <<< "$$@"; } ; \
		while read line; do p $$line | xargs; done'

strings: strings-fr
strings-%: src.dir = stopcovid/src/main/assets/Strings
strings-%:
	curl -s https://app.tousanticovid.gouv.fr/json/version-31/Strings/$@.json \
		> $(src.dir)/$@.json
	git diff $(src.dir)


fame:
	curl -s 'https://api.github.com/repos/ofa-/stopcovid-android/releases?page=1&per_page=100' \
	| jq '.[] | [ .created_at, .assets[0].download_count, .tag_name ] | @csv' \
	| tr -d '"\\' | cut -c -10,21- | tr , '\t'


keystore = stopcovid/my-release-key.jks

build: $(keystore) local.properties .
	./gradlew assembleRelease -x lintVitalRelease

install:
	./gradlew installRelease

install.downgrade:
	adb install -d -r ./stopcovid/build/outputs/apk/release/stopcovid-release.apk


$(keystore):
	echo "password,password,,,,,,,yes" | tr , "\n" |\
	keytool -genkey -v -keystore $(keystore) \
		-keyalg RSA -keysize 2048 -validity 10000 -alias my-alias

local.properties:
	cp $@.sample $@

build: submodule
submodule:
	git submodule update --init
