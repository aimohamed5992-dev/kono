# كونو · Kono

منصة توصيل متعددة الفئات للسوق السعودي على طراز «هنقرستيشن»:
**مطاعم + مقاضي (بندة، لولو، التميمي) + صيدليات + حلويات وورود ومياه.**

ثلاث طبقات:

| الطبقة | التقنية | المجلد | التشغيل |
|---|---|---|---|
| **تطبيق iOS** | SwiftUI (iOS 17+) | `App/` + `Kono.xcodeproj` | افتح المشروع في Xcode ← Run |
| **الواجهة الخلفية** | ASP.NET Core 10 · EF Core 9 · Pomelo · MySQL/MariaDB | `backend/` | `dotnet run --project backend/src/Kono.Api` → :5210 |
| **لوحة الإدارة** | React (Vite) · Tailwind · React Query | `admin/` | `cd admin && npm i && npm run dev` → :5173 |

## التشغيل الكامل
```bash
# 1) قاعدة البيانات (XAMPP MariaDB على 3306 كافٍ) — أنشئ قاعدة kono
# 2) الـ API  (يطبّق الهجرات ويبذر تلقائياً)
cd backend && dotnet run --project src/Kono.Api

# 3) لوحة الإدارة
cd admin && npm install && npm run dev        # http://localhost:5173

# 4) تطبيق iOS — من Xcode، أو:
SIMCTL_CHILD_KONO_API=http://localhost:5210 \
  xcrun simctl launch "iPhone 16 Pro" sa.kono.app
```

## الحسابات التجريبية
كل الحسابات كلمة مرورها `Kono@12345`:
- **مدير** (لوحة الإدارة): `+966500000000`
- **عميل**: `+966551234567`
- **مندوب**: `+966560000001`

التطبيق يسجّل الدخول تلقائياً بالحساب المناسب للدور المختار.

## المسار
- تصميم التطبيق: ٨ خطوات — كل شاشات العميل/المتجر/المندوب + هوية + أنيميشن + صور واقعية عبر Manus.
- الباك اند + لوحة الإدارة + الربط: ١٠ خطوات — كلها في `backend/README.md` و`admin/README.md`.

## الربط (الخطوة ١٠)
- `App/Networking/` — `KonoAPI` (async, JWT + تجديد)، DTOs، `AppData` (بوابة واحدة، fallback لـ `Mock` عند عدم الاتصال).
- الشاشات المتصلة: الرئيسية، صفحة القسم، المتجر، البحث، الدفع (تسعير من الخادم)، التتبّع (polling)، طلباتي، لوحة المتجر، شاشة المندوب.
- **تحقق end-to-end**: طلب من التطبيق → يظهر فوراً في تطبيق المتجر و`/api/admin/stats`.
- الصور: `AssetImage` يستخدم النسخة المحلية من الحزمة أولاً ثم الرابط البعيد.
