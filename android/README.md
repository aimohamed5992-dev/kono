# كونو · Kono — Android

تطبيق أندرويد (Kotlin + Jetpack Compose) بنفس تصميم ونفس شاشات تطبيق iOS:
**العميل · التاجر · المندوب** — عربي RTL أولاً مع تبديل إلى English.

| | |
|---|---|
| اللغة / الواجهة | Kotlin 2.0 · Jetpack Compose (Material 3 كقاعدة فقط، التصميم كله مخصّص) |
| الشبكة | OkHttp + kotlinx.serialization (نفس واجهة الـ API للـ iOS والويب) |
| الخرائط | osmdroid / OpenStreetMap — بدون مفتاح API |
| الصور | Coil + نسخة مضمّنة من صور المتاجر والأصناف (48 صورة webp) |
| الحد الأدنى | Android 8.0 (API 26) |

## التشغيل
```bash
# 1) قاعدة البيانات (XAMPP MariaDB) والـ API — انظر README الرئيسي
cd backend && dotnet run --project src/Kono.Api          # http://localhost:5210

# 2) المحاكي (يصل للـ API عبر 10.0.2.2)
~/Library/Android/sdk/emulator/emulator -avd Kono_Pixel &

# 3) البناء والتثبيت
cd android
./gradlew :app:installDebug                               # نسخة debug (~40MB)
./gradlew :app:assembleRelease                            # نسخة مصغّرة R8 (~4MB) → app/build/outputs/apk/release/
```
عنوان الـ API يُضبط من `BuildConfig.API_BASE` في `app/build.gradle.kts` (الافتراضي `http://10.0.2.2:5210`؛ لجهاز حقيقي ضع IP جهازك).
نسخة release موقّعة بمفتاح الـ debug للتجربة فقط — استبدله بمفتاح حقيقي قبل النشر.

## الحسابات التجريبية (OTP = `1234`، أو أي ٤ أرقام)
- عميل: `551234567` · مندوب: `560000001` · تاجر (بيتزا فورنو): `500000003` — أرقام سعودية بعد +966
- حساب تاجر/مندوب بدون طلب تسجيل سابق يفتح معالج التسجيل أولاً (زر «لاحقاً» يتجاوزه).

## البنية
```
app/src/main/java/sa/kono/app/
  data/        KonoApi (JWT + تجديد تلقائي) · AppData · CartStore · MerchantSession · CourierSession · نماذج الـ DTO
  ui/theme/    ألوان، خط Cairo، مسافات، ظلال دافئة، حركة
  ui/components/  أزرار، بطاقات، حقول، شريط التبويب العائم، الصور، الخرائط المصغّرة
  ui/auth · onboarding(Onboarding.kt) · customer · merchant · courier · chat · shared
  ui/i18n/     AppLanguage + Text — الترجمة من assets/en.json (نفس قاموس iOS)
```
> أي شاشة جديدة تستورد `sa.kono.app.ui.i18n.Text` (وليس material3 Text) حتى تُترجم تلقائياً.
