# نشر Kono API على Ubuntu VPS

الحزمة **self-contained لـ linux-x64** — لا تحتاج تثبيت .NET على السيرفر. (Ubuntu 22.04 / 24.04، معمارية x86_64.)
`InvariantGlobalization` مفعّل، فلا تحتاج مكتبة ICU.

## 1) قاعدة البيانات (مرة واحدة)
```bash
sudo apt update && sudo apt install -y mariadb-server nginx
sudo mysql <<'SQL'
CREATE DATABASE kono CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'kono'@'localhost' IDENTIFIED BY 'كلمة_سر_قوية';
GRANT ALL PRIVILEGES ON kono.* TO 'kono'@'localhost';
FLUSH PRIVILEGES;
SQL
```
التطبيق **ينشئ الجداول ويُطبّق الـ migrations ويبذر البيانات التجريبية تلقائياً** عند أول تشغيل (البذر فقط لو القاعدة فارغة).

## 2) رفع التطبيق
```bash
# من جهازك
scp kono-api-linux-x64.tar.gz user@SERVER:/tmp/
# على السيرفر
cd /tmp && tar xzf kono-api-linux-x64.tar.gz && cd kono-api
sudo ./deploy/install.sh
```
السكربت ينشئ المستخدم `kono`، ينسخ التطبيق إلى `/opt/kono-api`، يجعل مجلد الرفع `/var/lib/kono/uploads` (لا يُمسح عند الترقية)، يولّد `Jwt__Key` عشوائياً، ويسجّل خدمة systemd.

## 3) الإعدادات
```bash
sudo nano /etc/kono/kono-api.env
```
عدّل: `ConnectionStrings__Default` (كلمة سر القاعدة) · `App__PublicBaseUrl` (رابط الـ API العام https) · `Cors__AllowedOrigins__N` (دومين لوحة الإدارة/البوابة).
ثم:
```bash
sudo systemctl start kono-api
sudo journalctl -u kono-api -f          # تابع الإقلاع (المهاجرات + البذر)
curl http://127.0.0.1:5210/health       # {"status":"ok","database":"connected"}
```

## 4) nginx + HTTPS
```bash
sudo cp /tmp/kono-api/deploy/nginx-kono.conf /etc/nginx/sites-available/kono-api   # من المجلد المفكوك
sudo sed -i 's/api.example.com/YOUR_DOMAIN/' /etc/nginx/sites-available/kono-api
sudo ln -s /etc/nginx/sites-available/kono-api /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo apt install -y certbot python3-certbot-nginx && sudo certbot --nginx -d YOUR_DOMAIN
```
الإعداد يدعم WebSocket لـ SignalR على `/hub/` ورفع حتى 25MB (صور/رسائل صوتية الدردشة).
افتح المنافذ: `sudo ufw allow 'Nginx Full' && sudo ufw allow OpenSSH` — **لا تفتح 5210 ولا 3306** للخارج.

## 5) ربط التطبيقات بالسيرفر
- **Android**: غيّر `API_BASE` في `android/app/build.gradle.kts` إلى `https://YOUR_DOMAIN` ثم `./gradlew :app:assembleRelease`.
- **iOS**: متغيّر `KONO_API`، أو غيّر الافتراضي في `App/Networking/KonoAPI.swift` (iOS يشترط https).
- **لوحة الإدارة / البوابة**: غيّر proxy الـ API في `vite.config` أو ابنِ بـ `npm run build` وقدّم الملفات من nginx.

## ترقية لاحقاً
ابنِ الحزمة من جديد (`backend/publish.sh`)، ارفعها، فكّها، ثم `sudo ./deploy/install.sh` — يحافظ على ملف الإعدادات والملفات المرفوعة ويعيد تشغيل الخدمة.

## ملاحظات أمان
- حسابات التجربة (`+966500000000` …) و OTP الوهمي `1234` مناسبة للعرض فقط؛ قبل الإنتاج الفعلي بدّل OTP بمزوّد SMS وغيّر كلمات سر الحسابات المبذورة.
- Swagger مفعّل على `/swagger` (والجذر `/` يحوّل إليه). لإخفائه: قيّد المسار في nginx (`location /swagger { deny all; }`).
- النسخ الاحتياطي: `mysqldump kono` + مجلد `/var/lib/kono/uploads`.
- السجلات: `journalctl -u kono-api`.
