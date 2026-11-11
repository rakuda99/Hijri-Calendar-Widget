# 📅 Hijri Calendar Widget

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![License](https://img.shields.io/badge/License-MIT-blue)
![Language](https://img.shields.io/badge/Language-Java-orange?logo=openjdk)

**ودجت تقويم هجري متكامل لشاشة الأندرويد الرئيسية**

*A full-featured Hijri (Islamic) Calendar widget for Android home screen*

</div>

---

## ✨ المميزات / Features

- 📅 **تقويم هجري دقيق** – عرض التاريخ الهجري والميلادي بشكل فوري
- 🕌 **مواقيت الصلاة** – ودجت مستقل لأوقات الصلاة الخمس
- 📆 **ودجت الشهر الكامل** – عرض الشهر الهجري كاملاً
- 🔔 **تحديث تلقائي** – يتحدث عند منتصف الليل وعند تغيير التاريخ/الوقت
- ⚙️ **إعدادات متقدمة** – تخصيص الألوان، الخطوط، الحجم
- 💾 **نسخ احتياطية** – دعم استيراد وتصدير الإعدادات
- 🌐 **دعم ثنائي اللغة** – عربي وإنجليزي

---

## 📱 الودجتات المتاحة / Available Widgets

| الودجت | الوصف |
|--------|-------|
| **Hijri Calendar Widget** | ودجت التاريخ الرئيسي (هجري + ميلادي) |
| **Hijri Month Widget** | عرض الشهر الهجري كاملاً |
| **Prayer Widget** | أوقات الصلاة الخمس |

---

## 🛠️ متطلبات البناء / Build Requirements

- **Android Studio** Hedgehog أو أحدث
- **JDK 11** أو أحدث
- **Android SDK** API 26+ (Android 8.0)
- **Target SDK:** API 34 (Android 14)

---

## 🚀 طريقة البناء / How to Build

```bash
# استنساخ المشروع
git clone https://github.com/rakuda99/hijri-calendar-widget.git
cd hijri-calendar-widget

# بناء APK للتجربة
./gradlew assembleDebug

# تثبيت مباشرة على الجهاز
./gradlew installDebug
```

---

## 📂 هيكل المشروع / Project Structure

```
app/src/main/
├── java/com/example/hijricalendarwidget/
│   ├── HijriCalendarWidget.java      # الودجت الرئيسي
│   ├── HijriMonthWidget.java         # ودجت الشهر
│   ├── PrayerWidgetProvider.java     # ودجت الصلاة
│   ├── HijriDateUtils.java           # حسابات التاريخ الهجري
│   ├── SyncReceiver.java             # مستقبل التحديث الدوري
│   ├── SettingsActivity.java         # الإعدادات الرئيسية
│   └── ...
├── res/
│   ├── layout/                       # ملفات التخطيط
│   ├── xml/                          # معلومات الودجتات
│   └── drawable/                     # الأيقونات والأشكال
└── AndroidManifest.xml
```

---

## 🤝 المساهمة / Contributing

المساهمات مرحب بها! اتبع الخطوات التالية:

1. Fork المشروع
2. أنشئ فرعاً جديداً: `git checkout -b feature/my-feature`
3. اعمل تعديلاتك وأضفها: `git commit -m "Add my feature"`
4. ادفع الفرع: `git push origin feature/my-feature`
5. افتح Pull Request

---

## 📄 الرخصة / License

هذا المشروع مرخص تحت رخصة **MIT** – انظر ملف [LICENSE](LICENSE) للتفاصيل.

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

---

<div align="center">
صُنع بـ ❤️ | Made with ❤️
</div>
