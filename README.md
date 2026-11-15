# 📅 ودجت التقويم الهجري ومواقيت الصلاة | Hijri Calendar & Prayer Widget

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Java_%26_Kotlin-orange?style=for-the-badge&logo=java)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active_Development-brightgreen?style=for-the-badge)

**تطبيق وودجت أندرويد متكامل وشديد التخصيص يجمع بين التقويم الهجري والميلادي، ومواقيت الصلاة الدقيقة، وساعة رقمية أنيقة مع استهلاك فائق الكفاءة للبطارية.**

*A highly customizable Android Home Screen Widget combining Hijri & Gregorian calendars, precise prayer times calculation, travel-aware location updates, and digital clock.*

[✨ المميزات](#-المميزات--features) • [📱 الودجتات المتاحة](#-الودجتات-المتاحة--available-widgets) • [⚙️ المعمارية الفنية](#-المعمارية-الفنية--technical-architecture) • [🚀 طريقة التثبيت والبناء](#-طريقة-التثبيت-والبناء--how-to-build) • [🤝 المساهمة](#-المساهمة--contributing)

</div>

---

## ✨ المميزات / Features

- 📅 **تقويم هجري وميلادي دقيق:** تحديث تلقائي فوري عند منتصف الليل (00:00) دون الحاجة لفتح التطبيق.
- 🕌 **مواقيت الصلاة الدقيقة (Adhan Engine):** حساب أوقات الصلوات الخمس مع تمييز الصلاة القادمة وعد تنازلي دقيق بالدقائق والساعات.
- ✈️ **كشف السفر الذكي (Travel Detection):** إعادة حساب مواقيت الصلاة تلقائياً ومسح الكاش عند التنقل وتغير الموقع الجغرافي بأكثر من 5 كم.
- 📍 **اسم المدينة الحالية:** عرض موقع ومحافظة المستخدم الحالية تحت الساعة لضمان دقة الأوقات.
- 📴 **محرك حساب أوفلاين (100% Offline):** الحسابات الرياضية الفلكية للصلاة تتم على الجهاز مباشرة دون الحاجة لاتصال إنترنت دائم.
- 🎨 **تخصيص كامل للواجهة:** تعديل الألوان، تباين الشاشات (AMOLED Dark)، أحجام النصوص، وتنسيق الأرقام (أرقام عربية / إنجليزية).
- 🔋 **كفاءة عالية في استهلاك البطارية:** يعتمد على تنبيهات النظام الدقيقة والأحداث (`Exact Alarm` & `BroadcastReceiver`) دون تشغيل خدمات خلفية مستنزفة للبطارية.
- 🌐 **دعم كامل للغتين:** واجهة عربية كاملة + واجهة إنجليزية (Bilingual).

---

## 📱 الودجتات المتاحة / Available Widgets

| الودجت | الوصف |
| :--- | :--- |
| **ودجت التقويم والساعة (Clock & Hijri)** | يعرض الساعة الرقمية الكبيرة، اسم اليوم، التاريخ الهجري، التاريخ الميلادي، واسم المدينة. |
| **ودجت مواقيت الصلاة الأفقي (Horizontal)** | شريط مدمج لشاشة البداية يعرض الصلوات الخمس مع تمييز الصلاة القادمة والوقت المتبقي. |
| **ودجت مواقيت الصلاة الرأسي (Vertical)** | قائمة رأسية تفصيلية مناسبة للشاشات الكبيرة وجوانب الشاشة الرئيسية. |
| **ودجت الشهر الهجري الكامل (Month View)** | شبكة تقويم تعرض كامل أيام الشهر الهجري مع تمييز اليوم الحالي. |

---

## ⚙️ المعمارية الفنية / Technical Architecture

- **UI Framework:** Android AppWidget Framework (`RemoteViews`) متوافق مع Android 8.0 (API 26) حتى Android 14+ (API 34).
- **Prayer Engine:** خوارزميات حساب مواقيت الصلاة الفلكية (أم القرى، رابطة العالم الإسلامي، الهيئة المصرية، إلخ).
- **Location & Geofencing:** استخدام `FusedLocation / LocationManager` مع حساب المسافات الجغرافية (Haversine Formula) لكشف التنقلات والسفر.
- **Cache Management:** نظام `CacheManager` ذكي يفصل بين أوقات الصلاة، الطقس، والإحداثيات لتقليل استهلاك موارد الجهاز.

---

## 🚀 طريقة التثبيت والبناء / How to Build

### المتطلبات الأساسية:
- **Android Studio** (Hedgehog 2023.1.1 أو أحدث)
- **JDK 17** أو JDK 11
- **Android SDK:** API 26+ (الهدف: API 34)

### خطوات البناء:
```bash
# 1. استنساخ المستودع
git clone https://github.com/rakuda99/Hijri-Calendar-Widget.git
cd Hijri-Calendar-Widget

# 2. بناء ملف التجربة (Debug APK)
./gradlew assembleDebug

# 3. التثبيت المباشر على الهاتف المتصل
./gradlew installDebug
```

---

## 🤝 المساهمة / Contributing

المشروع مفتوح المصدر ونرحب بمساهمات المطورين لتحسين الأداء أو إضافة تصاميم ومميزات جديدة!

1. اعمل Fork للمستودع.
2. أنشئ فرع جديد للتعديل:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. احفظ التعديلات:
   ```bash
   git commit -m "feat: Add amazing feature"
   ```
4. ارفع الفرع:
   ```bash
   git push origin feature/amazing-feature
   ```
5. افتح **Pull Request**.

---

## 📄 الرخصة / License

هذا المشروع مرخص تحت رخصة **MIT License** — طالع ملف [LICENSE](LICENSE) لمزيد من التفاصيل.

---

<div align="center">
صُنع بإتقان و ❤️ لخدمة المستخدمين حول العالم
</div>
