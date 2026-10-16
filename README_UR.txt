Smart IPTV TV — Android TV Box APK project

یہ Android Studio کا native Android project ہے، تیار شدہ APK نہیں۔ اس ماحول میں Android SDK/Gradle build tools دستیاب نہیں تھے، اس لیے APK compile نہیں ہو سکی۔

خصوصیات:
- Android TV / TV Box کے لیے Leanback launcher entry
- ریموٹ کے 0–9 بٹن سے نمبر داخل کریں، پھر OK/Enter دبائیں
- چینل نمبر 1–9999
- چینل فہرست پر دیر تک OK/Select دباکر چینل نمبر تبدیل کریں؛ نمبر منفرد ہونا ضروری ہے
- M3U URL لوڈ کرنا یا فائل منتخب کرنا
- Favorites اور پچھلا/اگلا چینل
- چینل نمبر اور پلے لسٹ مقامی طور پر محفوظ
- Media3 ExoPlayer کے ذریعے ویڈیو چلانے کی کوشش

APK بنانے کا طریقہ:
1. کمپیوٹر پر Android Studio انسٹال کریں۔
2. Android Studio میں Open منتخب کریں اور اس ZIP کو Extract کرکے Smart_IPTV_AndroidTV_Project فولڈر کھولیں۔
3. Gradle sync مکمل ہونے دیں؛ اگر SDK 35 نہیں ہے تو Android Studio کی SDK Manager سے Android SDK Platform 35 انسٹال کریں۔
4. Build > Build Bundle(s) / APK(s) > Build APK(s) منتخب کریں۔
5. APK عام طور پر app/build/outputs/apk/debug/app-debug.apk میں بنے گی۔
6. APK کو USB یا کسی محفوظ طریقے سے Android TV Box میں منتقل کرکے انسٹال کریں۔

نوٹ:
- آپ کو اپنی قانونی IPTV M3U playlist درکار ہوگی؛ ایپ چینل مہیا نہیں کرتی۔
- کچھ TV ریموٹ نمبر کے بٹن Android کو عام KEYCODE_0…KEYCODE_9 کے بجائے مختلف codes دیتے ہیں؛ اصل Box پر ٹیسٹ کرنا ضروری ہے۔
- یہ starter project ہے؛ APK build اور playback کی اصل ڈیوائس پر تصدیق ابھی نہیں ہوئی۔


آن لائن مفت Build کا متبادل:
1. اس فولڈر کے تمام مواد کو اپنی GitHub repository میں upload کریں۔
2. GitHub میں Actions کھولیں اور "Build Android TV APK" workflow چلائیں۔
3. Build کامیاب ہونے پر workflow کے Artifacts سے Smart-IPTV-TV-debug-apk.zip ڈاؤن لوڈ کریں؛ اس کے اندر app-debug.apk ہوگی۔
4. یہ خودکار build ہے؛ Android TV Box پر ریموٹ اور اسٹریمنگ کی عملی جانچ پھر بھی ضروری ہے۔
