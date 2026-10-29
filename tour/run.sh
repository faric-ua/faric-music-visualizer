set -x
mkdir -p out
adb install -r -g apk/*.apk > out/install.txt 2>&1
for p in android.permission.READ_MEDIA_AUDIO android.permission.POST_NOTIFICATIONS android.permission.RECORD_AUDIO; do adb shell pm grant com.saney.musicvisualizer $p || true; done
adb shell mkdir -p "/sdcard/Music/Audio Converter"
adb push tour/music/. "/sdcard/Music/Audio Converter/" > out/push.txt 2>&1
adb shell content call --uri content://media/ --method scan_volume --arg external_primary > out/scan.txt 2>&1 || true
sleep 8
adb shell content query --uri content://media/external/audio/media --projection _display_name:album > out/mediastore.txt 2>&1 || true
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
sleep 45
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true
python3 tour/drive.py tour/plan.txt
adb logcat -d -t 3000 > out/logcat.txt 2>&1 || true
adb shell dumpsys activity activities | head -80 > out/activities.txt 2>&1 || true
ls -la out
