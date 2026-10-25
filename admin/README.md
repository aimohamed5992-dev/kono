# كونو — لوحة الإدارة (Admin)

React (Vite) + Tailwind v4 + React Router + TanStack Query + Recharts.
هوية كونو (أصفر/بني)، عربي RTL.

## التشغيل
```bash
cd admin
npm install
npm run dev        # http://localhost:5173  (بروكسي /api و /img → :5210)
```
تأكد أن الـ API يعمل على `http://localhost:5210`.

## الدخول
حساب المدير المبذور: `+966500000000` / `Kono@12345`
(الدخول مسموح فقط للحسابات بدور `Admin`.)

## الإعداد
- `VITE_API_URL` — عنوان الـ API إن لم يكن نفس الأصل (افتراضياً بروكسي Vite).
- `VITE_API_TARGET` — هدف بروكسي التطوير (افتراضي `http://localhost:5210`).

## البنية
```
src/
  lib/
    api.js        axios + إرفاق التوكن + تجديد تلقائي عند 401
    auth.jsx      AuthProvider / useAuth (login/logout/me)
  components/
    Layout.jsx    شريط جانبي + علوي
    ui.jsx        Button/Card/Badge/Input/Select/PageHeader/…
  pages/
    Login.jsx
    Dashboard.jsx (خطوة ٨) …
```

## الصفحات
| المسار | الحالة |
|---|---|
| `/login` | ✅ |
| `/` لوحة التحكم — KPIs + رسم Recharts + الطلبات حسب الحالة + أفضل الأصناف/المتاجر | ✅ |
| `/orders` الطلبات — تبويبات + جدول + Drawer تفاصيل (سجل مراحل + تغيير حالة) | ✅ |
| `/stores` المتاجر — جدول + بحث/فلتر + Modal إنشاء/تعديل + إغلاق/حذف | ✅ |
| `/products` المنتجات — اختيار متجر + قائمة بالتصنيفات + Modal صنف + تفعيل/إيقاف/حذف | ✅ |
| `/couriers` المناديب — جدول + Modal تعديل | ✅ |
| `/customers` العملاء — جدول + بحث + إيقاف/تفعيل | ✅ |
| `/verticals` الأقسام — ٦ بطاقات تحرير مباشر | ✅ |
| `/promotions` العروض — بطاقات ملوّنة + Modal + color picker + معاينة | ✅ |
