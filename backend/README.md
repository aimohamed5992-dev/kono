# كونو — الواجهة الخلفية (Kono Backend)

ASP.NET Core **10** Web API + **EF Core 9** + **Pomelo.EntityFrameworkCore.MySql** على **MySQL/MariaDB**.

## المتطلبات
- .NET SDK 10
- MySQL أو MariaDB يعمل على `127.0.0.1:3306` (XAMPP كافٍ)
- قاعدة بيانات باسم `kono` (تُنشأ تلقائياً بالهجرات، أو يدوياً):
  ```sql
  CREATE DATABASE kono CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

## الإعداد
سلسلة الاتصال في `src/Kono.Api/appsettings.json` → `ConnectionStrings:Default`
(الافتراضي: XAMPP، المستخدم `root` بدون كلمة مرور).

## التشغيل
```bash
cd backend
dotnet run --project src/Kono.Api      # http://localhost:5210  ·  Swagger على /
```

فحص الصحة: `GET /health` → يتأكد من الاتصال بقاعدة البيانات.

## إعادة ضبط البيانات
```sql
DROP DATABASE kono; CREATE DATABASE kono CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
ثم أعد تشغيل الـ API (يطبّق الهجرات ويبذر من جديد).

## الهجرات (EF Core)
```bash
dotnet tool install --global dotnet-ef      # مرة واحدة
cd backend/src/Kono.Api
dotnet ef migrations add <Name>
dotnet ef database update
```

## البنية
```
backend/
  Kono.slnx
  src/Kono.Api/
    Program.cs          نقطة الدخول، DI، الأنابيب
    appsettings.json    الاتصال، JWT، CORS
    Data/               KonoDbContext + إعدادات الكيانات
    Domain/             الكيانات (خطوة ٢)
    Dtos/               نماذج نقل البيانات
    Controllers/        نقاط النهاية
    Services/           منطق الأعمال
    Auth/               JWT، الأدوار
```

## بيانات الدخول التجريبية (بعد البذر)
كل الحسابات كلمة مرورها `Kono@12345`:
- مدير: `+966500000000`
- عميل: `+966551234567` (أحمد المطيري)
- مندوب: `+966560000001` (سعود الحربي)

## خطة التنفيذ (١٠ خطوات)
1. ✅ هيكلة المشروع + الاتصال بقاعدة البيانات + Swagger + فحص الصحة
2. ✅ نموذج المجال (١٦ جدول) + الهجرة الأولى + بذر تلقائي عند الإقلاع + خدمة الصور من `wwwroot/img`
3. ✅ المصادقة: JWT + refresh tokens دوّارة، `POST /api/auth/{register,login,refresh,logout}` · `GET /api/auth/me` · تطبيع رقم الجوال · أخطاء ProblemDetails عربية · Swagger bearer
4. ✅ الكتالوج: `GET /api/{verticals,promotions,home}` · `GET /api/stores?kind=&search=&sort=&freeDelivery=&openNow=&promo=&page=&pageSize=` · `GET /api/stores/{id}` (قائمة كاملة) · `GET /api/search?q=&scope=` — DTOs، ترقيم صفحات، روابط صور مطلقة، `/api/home` يرجّع كل أقسام الرئيسية
5. ✅ الطلبات: `POST /api/orders` (تسعير من الخادم) · `GET /api/orders?status=` · `GET /api/orders/{id}` · `POST /api/orders/{id}/cancel` · **المتجر**: `GET /api/merchant/tickets` + `accept/advance/reject` · **المندوب**: `POST /api/courier/status` · `GET /api/courier/{offers,jobs/active}` + `offers/{id}/accept` + `jobs/{id}/advance` — آلة حالات واحدة (`OrderStage`) + سجل أحداث + حماية بالأدوار
6. ✅ الإدارة `[Authorize(Roles=Admin)]` تحت `/api/admin`: `stats` (إيرادات يوم/٧/٣٠، طلبات حسب الحالة، إيراد يومي، أفضل الأصناف والمتاجر) · `orders` (قائمة/تفاصيل/تغيير مرحلة) · **CRUD** للمتاجر والمنتجات والتصنيفات · المناديب · العملاء (تعطيل) · الأقسام · العروض
7. ✅ لوحة الإدارة React: الهيكل، تسجيل الدخول، التخطيط، عميل API (تجديد توكن تلقائي)
8. ✅ لوحة الإدارة: لوحة تحكم (Recharts) + الطلبات (Drawer) + المتاجر (CRUD) + المنتجات
9. ✅ لوحة الإدارة: المناديب + العملاء + الأقسام (تحرير مباشر) + العروض
10. ✅ ربط تطبيق iOS بالـ API (`App/Networking/`: KonoAPI + DTOs + AppData) + fallback لـ Mock + تحقق e2e
```
