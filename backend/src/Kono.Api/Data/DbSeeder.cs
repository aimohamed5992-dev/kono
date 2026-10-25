using Kono.Api.Domain;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Data;

/// <summary>Idempotent dev seed — mirrors the Kono iOS mock content.</summary>
public static class DbSeeder
{
    private const string Pwd = "Kono@12345";   // every seeded account

    public static async Task SeedAsync(IServiceProvider sp)
    {
        var db = sp.GetRequiredService<KonoDbContext>();
        if (await db.Verticals.AnyAsync()) return;

        var hash = BCrypt.Net.BCrypt.HashPassword(Pwd);

        // ── Verticals ────────────────────────────────────────────────────────
        db.Verticals.AddRange(
            V(VerticalKind.Restaurants, "مطاعم", "وجبتك المفضلة توصلك سريع", "fork.knife", "cat_restaurants", 0),
            V(VerticalKind.Groceries, "بقالة", "مقاضي البيت من بندة ولولو والتميمي", "basket.fill", "cat_groceries", 1),
            V(VerticalKind.Pharmacy, "صيدليات", "دواءك يوصلك خلال دقائق", "cross.case.fill", "cat_pharmacy", 2),
            V(VerticalKind.Sweets, "حلويات", "حلا ومناسبات", "birthday.cake.fill", "cat_sweets", 3),
            V(VerticalKind.Flowers, "ورود", "بوكيهات وهدايا", "leaf.fill", "cat_flowers", 4),
            V(VerticalKind.Water, "مياه", "قوارير ومياه معدنية", "drop.fill", "cat_water", 5));

        // ── Promotions ───────────────────────────────────────────────────────
        db.Promotions.AddRange(
            new Promotion { TitleAr = "توصيل مجاني", SubtitleAr = "على أول ٣ طلبات من المطاعم المشاركة", ImageUrl = Img("promo_free_delivery"), BackgroundHex = "#FFE79A", SortOrder = 0 },
            new Promotion { TitleAr = "اشترك في كونو بلس", SubtitleAr = "ووفّر رسوم التوصيل طوال الشهر", ImageUrl = Img("promo_konoplus"), BackgroundHex = "#FFEFC2", SortOrder = 1 },
            new Promotion { TitleAr = "خصم ٣٠٪ على المقاضي", SubtitleAr = "من بندة ولولو — حتى نهاية الأسبوع", ImageUrl = Img("promo_grocery_sale"), BackgroundHex = "#E9F5D8", SortOrder = 2 });

        // ── Users ────────────────────────────────────────────────────────────
        var admin = U("+966500000000", "مدير كونو", UserRole.Admin, hash, "admin@kono.sa");
        var customer = U("+966551234567", "أحمد المطيري", UserRole.Customer, hash, "ahmed@example.com");
        customer.KonoPlus = true; customer.WalletBalance = 84; customer.Points = 1240;
        var customer2 = U("+966555555501", "نورة العتيبي", UserRole.Customer, hash, "noura@example.com");
        db.Users.AddRange(admin, customer, customer2);

        customer.Addresses.AddRange(
            A("المنزل", "حي الياسمين، شارع الأمير سلطان", "house.fill", 24.813, 46.637, true),
            A("العمل", "طريق الملك فهد، برج المملكة", "briefcase.fill", 24.711, 46.674, false),
            A("بيت الوالدة", "حي النرجس، شارع أنس بن مالك", "heart.fill", 24.842, 46.651, false));
        customer.PaymentMethods.AddRange(
            P("Apple Pay", "apple.logo", null, true),
            P("مدى", "creditcard.fill", "**** 4421", false),
            P("محفظة كونو", "wallet.pass.fill", "84.00 ر.س", false),
            P("الدفع عند الاستلام", "banknote.fill", "نقداً", false));

        // couriers
        var couriers = new[]
        {
            MakeCourier(db, "+966560000001", "سعود الحربي", "ن ح ر · ٤٨٢١", "courier_1", hash, 4.9, 2140, 24.802, 46.641),
            MakeCourier(db, "+966560000002", "ماجد الشمري", "أ ب ج · ١٢٣٤", "courier_1", hash, 4.8, 1560, 24.78, 46.70),
            MakeCourier(db, "+966560000003", "فيصل القحطاني", "د هـ و · ٥٦٧٨", "courier_1", hash, 4.7, 980, 24.75, 46.66),
        };

        await db.SaveChangesAsync();

        // ── Stores + menus ───────────────────────────────────────────────────
        var stores = new List<Store>
        {
            S("برجرايزر", VerticalKind.Restaurants, "برجر · أمريكي", 4.7, 3120, 25, 0, 25, 1.8, "توصيل مجاني", "store_burger", new[] { "الأكثر طلباً", "برجر لحم" }),
            S("شاورما هوس", VerticalKind.Restaurants, "شاورما · لبناني", 4.5, 1890, 30, 5, 20, 2.6, "خصم ٢٥٪", "store_shawarma", new[] { "شاورما دجاج" }, priceLevel: 1),
            S("بيتزا فورنو", VerticalKind.Restaurants, "بيتزا · إيطالي", 4.6, 2210, 35, 8, 40, 3.9, null, "store_pizza", new[] { "عجينة طازجة" }, priceLevel: 3),
            S("كوفي رومرز", VerticalKind.Restaurants, "قهوة مختصة · حلا", 4.8, 980, 20, 6, 15, 1.2, null, "store_coffee", new[] { "مختص" }, busy: true),
            S("دجاج تكساس", VerticalKind.Restaurants, "دجاج مقلي · أمريكي", 4.4, 2740, 28, 0, 30, 2.2, "توصيل مجاني", "store_shawarma", new[] { "دجاج مقرمش" }),
            S("بيك", VerticalKind.Restaurants, "وجبات سريعة · سعودي", 4.9, 12800, 22, 4, 15, 0.9, null, "store_burger", new[] { "الأكثر طلباً" }, priceLevel: 1),
            S("كافيه أرابيكا", VerticalKind.Restaurants, "قهوة · مخبوزات", 4.7, 640, 18, 6, 20, 1.5, "خصم ٢٠٪", "store_coffee", new[] { "كرواسون" }),
            S("بندة", VerticalKind.Groceries, "سوبرماركت", 4.4, 5400, 40, 10, 50, 4.5, "خصم على أول طلب", "store_panda", new[] { "مقاضي البيت" }),
            S("لولو هايبر ماركت", VerticalKind.Groceries, "هايبر ماركت", 4.5, 4100, 45, 12, 60, 6.1, null, "store_lulu", new[] { "تشكيلة واسعة" }),
            S("التميمي ماركت", VerticalKind.Groceries, "سوبرماركت", 4.3, 2600, 38, 9, 45, 3.4, null, "store_tamimi", Array.Empty<string>()),
            S("كارفور", VerticalKind.Groceries, "هايبر ماركت", 4.4, 3900, 50, 12, 70, 7.0, "خصم ٣٠٪ اليوم", "store_lulu", Array.Empty<string>()),
            S("صيدلية النهدي", VerticalKind.Pharmacy, "صيدلية", 4.7, 3300, 18, 7, 0, 1.1, "توصيل خلال ٢٠ دقيقة", "store_nahdi", new[] { "دواء", "عناية" }),
            S("صيدلية الدواء", VerticalKind.Pharmacy, "صيدلية", 4.6, 2100, 22, 7, 0, 2.0, null, "store_dawaa", new[] { "عناية بالطفل" }),
            S("صيدلية المتحدة", VerticalKind.Pharmacy, "صيدلية", 4.5, 1200, 25, 5, 0, 2.8, null, "store_nahdi", new[] { "فيتامينات" }),
            S("حلويات سعد الدين", VerticalKind.Sweets, "حلويات شرقية", 4.6, 1500, 40, 10, 30, 5.0, null, "store_sweets", new[] { "كنافة", "بقلاوة" }),
            S("بيت الكيك", VerticalKind.Sweets, "كيك ومناسبات", 4.8, 890, 45, 15, 50, 4.2, null, "store_sweets", new[] { "كيك مناسبات" }, priceLevel: 3),
        };

        // Spread the stores across real Riyadh districts so the home feed and
        // "nearby" genuinely change with the customer's chosen delivery address.
        // Ordered so the first few (used by the demo customer's live + past orders)
        // sit near the seeded "home" address in حي الياسمين (24.813, 46.637).
        var districts = new (string ar, double lat, double lng)[]
        {
            ("حي الياسمين", 24.8330, 46.6390),
            ("حي الملقا", 24.8130, 46.6110),
            ("حي الصحافة", 24.8060, 46.6360),
            ("حي الغدير", 24.7900, 46.6480),
            ("حي النرجس", 24.8620, 46.6480),
            ("حي المروج", 24.7720, 46.6540),
            ("حي العقيق", 24.7690, 46.6250),
            ("حي النخيل", 24.7490, 46.6300),
            ("حي الملك فهد", 24.7550, 46.6650),
            ("حي حطين", 24.7530, 46.6000),
            ("حي الورود", 24.7150, 46.6720),
            ("حي الازدهار", 24.7830, 46.7180),
            ("حي العليا", 24.6903, 46.6858),
            ("حي قرطبة", 24.8080, 46.7690),
            ("حي الربوة", 24.7100, 46.7500),
            ("حي الملز", 24.6650, 46.7360),
        };
        for (int i = 0; i < stores.Count; i++)
        {
            var d = districts[i % districts.Length];
            stores[i].Latitude = d.lat;
            stores[i].Longitude = d.lng;
            stores[i].AddressAr = $"{d.ar}، الرياض";
            stores[i].CityAr = "الرياض";
            stores[i].IbanLast4 = (4821 + i * 37 % 9000).ToString().PadLeft(4, '0')[^4..];
            stores[i].OpeningHoursAr = stores[i].Kind == VerticalKind.Pharmacy
                ? "٢٤ ساعة"
                : "١٠:٠٠ ص – ١:٠٠ ص";
        }

        foreach (var s in stores)
        {
            BuildMenu(s);
            db.Stores.Add(s);
        }
        await db.SaveChangesAsync();

        // ── Every store gets its own merchant owner ──────────────────────────
        // Phone scheme: +96650000000N (N = 1-based store index; +966500000000 is admin).
        // The customer→merchant demo flow logs into the owner of the store the
        // customer last ordered from (POST /api/auth/demo-store-merchant).
        for (int i = 0; i < stores.Count; i++)
        {
            var phone = $"+96650000{(i + 1):D4}";
            var owner = U(phone, $"{stores[i].Name} — إدارة", UserRole.Merchant, hash);
            db.Users.Add(owner);
            stores[i].Owner = owner;
        }
        await db.SaveChangesAsync();

        // ── One in-flight order for the demo customer ────────────────────────
        var burgerayzer = stores[0];
        await db.Entry(burgerayzer).Collection(x => x.Products).LoadAsync();
        var order = new Order
        {
            ShortCode = "KON-4821",
            CustomerUserId = customer.Id,
            StoreId = burgerayzer.Id,
            CourierId = couriers[0].Id,
            Stage = OrderStage.OnTheWay,
            PlacedAt = DateTime.UtcNow.AddMinutes(-18),
            IsDelivery = true,
            PaymentLabelAr = "Apple Pay",
            AddressLabelAr = "المنزل",
            AddressLineAr = "حي الياسمين، شارع الأمير سلطان",
            AddressLat = 24.813, AddressLng = 46.637,
        };
        foreach (var p in burgerayzer.Products.Take(3))
            order.Items.Add(new OrderItem
            {
                ProductId = p.Id, NameAr = p.NameAr, ImageUrl = p.ImageUrl,
                UnitPrice = p.Price, Quantity = 1, LineTotal = p.Price
            });
        Recalculate(order, burgerayzer);
        foreach (var st in new[] { OrderStage.Placed, OrderStage.Confirmed, OrderStage.Preparing, OrderStage.ReadyForPickup, OrderStage.PickedUp, OrderStage.OnTheWay })
            order.Events.Add(new OrderEvent { Stage = st, At = order.PlacedAt.AddMinutes((int)st * 2) });
        db.Orders.Add(order);

        // ── Completed order history (last 7 days) so reports/earnings are real ─
        var histStores = new[] { stores[0], stores[1], stores[4], stores[7], stores[11] };
        foreach (var s in histStores)
            if (s.Products.Count == 0)
                await db.Entry(s).Collection(x => x.Products).LoadAsync();

        var rnd = new Random(4821);
        int seq = 4200;
        var todayCourier = couriers[0];
        decimal todayEarnings = 0;
        int todayTrips = 0;
        double todayDistance = 0;

        for (int d = 6; d >= 0; d--)
        {
            var dayCount = d == 0 ? rnd.Next(3, 6) : rnd.Next(2, 5);
            for (int n = 0; n < dayCount; n++)
            {
                var s = histStores[rnd.Next(histStores.Length)];
                if (s.Products.Count == 0) continue;
                var placed = DateTime.UtcNow.AddDays(-d)
                    .AddHours(-rnd.Next(0, 8)).AddMinutes(-rnd.Next(0, 59));
                var courier = couriers[rnd.Next(couriers.Length)];
                // bias most trips to the demo courier so their history is rich
                if (rnd.Next(100) < 70) courier = todayCourier;

                var h = new Order
                {
                    ShortCode = $"KON-{seq++}",
                    CustomerUserId = customer.Id,
                    StoreId = s.Id,
                    CourierId = courier.Id,
                    Stage = OrderStage.Delivered,
                    PlacedAt = placed,
                    DeliveredAt = placed.AddMinutes(rnd.Next(28, 55)),
                    IsDelivery = true,
                    PaymentLabelAr = rnd.Next(2) == 0 ? "Apple Pay" : "بطاقة مدى",
                    AddressLabelAr = "المنزل",
                    AddressLineAr = "حي الياسمين، شارع الأمير سلطان",
                    AddressLat = 24.813, AddressLng = 46.637,
                    Tip = rnd.Next(3) == 0 ? rnd.Next(1, 4) * 5 : 0,
                };
                foreach (var p in s.Products.OrderBy(_ => rnd.Next()).Take(rnd.Next(1, 4)))
                    h.Items.Add(new OrderItem
                    {
                        ProductId = p.Id, NameAr = p.NameAr, ImageUrl = p.ImageUrl,
                        UnitPrice = p.Price, Quantity = rnd.Next(1, 3),
                        LineTotal = p.Price * rnd.Next(1, 3)
                    });
                Recalculate(h, s);
                foreach (var st in new[] { OrderStage.Placed, OrderStage.Confirmed, OrderStage.Preparing,
                                           OrderStage.ReadyForPickup, OrderStage.PickedUp, OrderStage.OnTheWay,
                                           OrderStage.Delivered })
                    h.Events.Add(new OrderEvent { Stage = st, At = placed.AddMinutes((int)st * 3) });
                db.Orders.Add(h);

                courier.Trips++;
                if (courier.Id == todayCourier.Id && d == 0)
                {
                    todayEarnings += h.CourierPayout + h.Tip;
                    todayTrips++;
                    todayDistance += Math.Round(1.5 + rnd.NextDouble() * 4, 1);
                }
            }
        }
        todayCourier.TodayEarnings = todayEarnings;
        todayCourier.TodayTrips = todayTrips;
        todayCourier.TodayDistanceKm = Math.Round(todayDistance, 1);

        // ── In-progress orders so the live operations map has something to show ─
        var liveStages = new[]
        {
            OrderStage.Confirmed, OrderStage.Preparing, OrderStage.ReadyForPickup,
            OrderStage.CourierAssigned, OrderStage.PickedUp, OrderStage.OnTheWay,
        };
        var dropSpots = new (double Lat, double Lng, string Line)[]
        {
            (24.8195, 46.6410, "حي الياسمين، شارع التخصصي"),
            (24.7743, 46.7386, "حي المروج، طريق الملك فهد"),
            (24.7050, 46.6750, "حي الملز، شارع جرير"),
            (24.6870, 46.7220, "حي الروضة، شارع خالد بن الوليد"),
            (24.7530, 46.6260, "حي العليا، طريق العروبة"),
            (24.8330, 46.6890, "حي النرجس، الدائري الشمالي"),
        };
        for (int i = 0; i < 6; i++)
        {
            var s = stores[i % stores.Count];
            if (s.Products.Count == 0) await db.Entry(s).Collection(x => x.Products).LoadAsync();
            if (s.Products.Count == 0) continue;

            var stage = liveStages[i % liveStages.Length];
            var drop = dropSpots[i % dropSpots.Length];
            var placed = DateTime.UtcNow.AddMinutes(-rnd.Next(6, 40));
            var courier = couriers[i % couriers.Length];

            var lo = new Order
            {
                ShortCode = $"KON-{seq++}",
                CustomerUserId = customer.Id,
                StoreId = s.Id,
                CourierId = (int)stage >= (int)OrderStage.CourierAssigned ? courier.Id : null,
                Stage = stage,
                PlacedAt = placed,
                IsDelivery = true,
                PaymentLabelAr = "Apple Pay",
                AddressLabelAr = "المنزل",
                AddressLineAr = drop.Line,
                AddressLat = drop.Lat, AddressLng = drop.Lng,
            };
            foreach (var p in s.Products.OrderBy(_ => rnd.Next()).Take(rnd.Next(1, 4)))
                lo.Items.Add(new OrderItem
                {
                    ProductId = p.Id, NameAr = p.NameAr, ImageUrl = p.ImageUrl,
                    UnitPrice = p.Price, Quantity = rnd.Next(1, 3), LineTotal = p.Price * rnd.Next(1, 3),
                });
            Recalculate(lo, s);
            foreach (var st in liveStages)
            {
                if ((int)st > (int)stage) break;
                lo.Events.Add(new OrderEvent { Stage = st, At = placed.AddMinutes((int)st * 2) });
            }
            db.Orders.Add(lo);

            // position an assigned courier somewhere sensible along the route
            if (lo.CourierId is not null)
            {
                double t = stage switch
                {
                    OrderStage.CourierAssigned => 0.15,
                    OrderStage.PickedUp => 0.05,
                    OrderStage.OnTheWay => 0.6,
                    _ => 0.3,
                };
                var from = stage >= OrderStage.PickedUp
                    ? (s.Latitude, s.Longitude)
                    : (s.Latitude + 0.01, s.Longitude + 0.01);
                courier.Latitude = from.Item1 + (drop.Lat - from.Item1) * t;
                courier.Longitude = from.Item2 + (drop.Lng - from.Item2) * t;
                courier.IsOnline = true;
            }
        }

        await db.SaveChangesAsync();
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private static string Img(string name) => $"/img/{name}.jpg";

    private static Vertical V(VerticalKind k, string t, string sub, string icon, string img, int sort) =>
        new() { Kind = k, TitleAr = t, SubtitleAr = sub, Icon = icon, ImageUrl = Img(img), SortOrder = sort };

    private static User U(string phone, string name, UserRole role, string hash, string? email = null) =>
        new() { Phone = phone, NameAr = name, Role = role, PasswordHash = hash, Email = email };

    private static Address A(string label, string line, string icon, double lat, double lng, bool def) =>
        new() { LabelAr = label, LineAr = line, Icon = icon, Latitude = lat, Longitude = lng, IsDefault = def };

    private static PaymentMethod P(string label, string icon, string? detail, bool def) =>
        new() { LabelAr = label, Icon = icon, DetailAr = detail, IsDefault = def };

    private static Courier MakeCourier(KonoDbContext db, string phone, string name, string plate,
        string photo, string hash, double rating, int trips, double lat, double lng)
    {
        var u = U(phone, name, UserRole.Courier, hash);
        var c = new Courier
        {
            User = u, Name = name, Rating = rating, Trips = trips, PlateAr = plate,
            PhotoUrl = photo, VehicleAr = "دباب", IsOnline = true, AcceptanceRate = 94,
            Latitude = lat, Longitude = lng, TodayTrips = 12, TodayEarnings = 214, TodayDistanceKm = 47.5
        };
        db.Couriers.Add(c);
        return c;
    }

    private static Store S(string name, VerticalKind kind, string cuisine, double rating, int ratingCount,
        int eta, decimal deliveryFee, decimal minOrder, double dist, string? promo, string hero,
        string[] tags, int priceLevel = 2, bool busy = false)
    {
        var s = new Store
        {
            Name = name, Kind = kind, CuisineAr = cuisine, Rating = rating, RatingCount = ratingCount,
            EtaMinutes = eta, DeliveryFee = deliveryFee, MinOrder = minOrder, DistanceKm = dist,
            PromoTagAr = promo, HeroImageUrl = Img(hero), LogoImageUrl = "", PriceLevel = priceLevel,
            IsBusy = busy, PrepMinutes = 15,
            AddressAr = "الرياض"          // real district coordinates are assigned after the list is built
        };
        foreach (var t in tags) s.Tags.Add(new StoreTag { Text = t });
        return s;
    }

    internal static void BuildMenu(Store s)
    {
        (string cat, (string n, string d, decimal price, decimal? old, string img, bool pop, int? cal, string? unit)[] items)[] plan = s.Kind switch
        {
            VerticalKind.Groceries => new[]
            {
                ("عروض اليوم", new[]
                {
                    ("حليب المراعي كامل الدسم", "عبوة ٢ لتر", 9.5m, (decimal?)12m, "grocery_milk", true, (int?)null, (string?)"٢ لتر"),
                    ("بيض المزرعة طازج", "كبير الحجم", 14m, (decimal?)17m, "grocery_eggs", false, null, "٣٠ حبة"),
                    ("موز إكوادوري", "طازج", 7.9m, null, "grocery_banana", false, null, "١ كجم"),
                }),
                ("خضار وفواكه", new[]
                {
                    ("طماطم", "درجة أولى", 5.5m, (decimal?)null, "grocery_tomato", false, (int?)null, (string?)"١ كجم"),
                    ("أرز بسمتي أبو كاس", "حبة طويلة", 42m, 49m, "grocery_rice", false, null, "٥ كجم"),
                }),
            },
            VerticalKind.Pharmacy => new[]
            {
                ("الأكثر طلباً", new[]
                {
                    ("بنادول أدفانس", "مسكن وخافض حرارة - ٢٤ قرص", 21m, (decimal?)null, "pharma_panadol", true, (int?)null, (string?)"٢٤ قرص"),
                    ("فيتامين سي ١٠٠٠", "فوار - ٢٠ قرص", 33m, 39m, "pharma_panadol", false, null, "٢٠ قرص"),
                }),
            },
            VerticalKind.Sweets => new[]
            {
                ("الأكثر طلباً", new[]
                {
                    ("كنافة نابلسية", "بالجبنة والقطر", 45m, (decimal?)null, "store_sweets", true, (int?)620, (string?)"٥٠٠ جم"),
                    ("بقلاوة مشكّلة", "فستق ولوز", 60m, null, "store_sweets", false, null, "١ كجم"),
                }),
            },
            _ => new[]  // restaurants + default
            {
                ("الأكثر طلباً", new[]
                {
                    ("برجر كلاسيك مزدوج", "قطعتا لحم أنجوس، جبن شيدر، صوص كونو الخاص", 32m, (decimal?)39m, "food_burger_double", true, (int?)780, (string?)null),
                    ("دجاج مقرمش سبايسي", "صدر دجاج مقرمش، مخلل، صوص حار", 27m, null, "food_chicken", true, 640, null),
                    ("بطاطس ودجز بالجبن", "بطاطس مقلية مع صوص الجبن والهالبينو", 15m, null, "food_fries", false, 420, null),
                }),
                ("برجر", new[]
                {
                    ("برجر مشروم و سويس", "لحم أنجوس، مشروم سوتيه، جبن سويسري", 34m, (decimal?)null, "food_burger_mushroom", false, (int?)720, (string?)null),
                    ("برجر باربكيو", "لحم، بصل مكرمل، صوص باربكيو، حلقات بصل", 36m, null, "food_burger_bbq", false, 810, null),
                }),
                ("مشروبات", new[]
                {
                    ("ميلك شيك فانيلا", "حليب طازج، آيس كريم فانيلا، كريمة", 18m, (decimal?)null, "food_shake", false, (int?)390, (string?)null),
                }),
            },
        };

        int catOrder = 0;
        foreach (var (catName, items) in plan)
        {
            var cat = new MenuCategory { NameAr = catName, SortOrder = catOrder++ };
            int pOrder = 0;
            foreach (var it in items)
            {
                var product = new Product
                {
                    NameAr = it.n, DescAr = it.d, Price = it.price, OldPrice = it.old,
                    ImageUrl = Img(it.img), IsPopular = it.pop, Calories = it.cal, UnitAr = it.unit,
                    SortOrder = pOrder++, SoldCount = Random.Shared.Next(8, 40)
                };
                if (s.Kind == VerticalKind.Restaurants && catName == "برجر")
                {
                    product.OptionGroups.Add(new ProductOptionGroup
                    {
                        TitleAr = "الحجم", Required = true, MultiSelect = false, SortOrder = 0,
                        Choices = { new() { NameAr = "عادي" }, new() { NameAr = "وجبة (بطاطس + مشروب)", PriceDelta = 12 } }
                    });
                    product.OptionGroups.Add(new ProductOptionGroup
                    {
                        TitleAr = "إضافات", Required = false, MultiSelect = true, SortOrder = 1,
                        Choices = { new() { NameAr = "جبن إضافي", PriceDelta = 4 }, new() { NameAr = "بيكون", PriceDelta = 6 } }
                    });
                }
                cat.Products.Add(product);
                s.Products.Add(product);
            }
            s.Categories.Add(cat);
        }
    }

    private static void Recalculate(Order o, Store s)
    {
        o.Subtotal = o.Items.Sum(i => i.LineTotal);
        o.DeliveryFee = s.DeliveryFee;
        o.ServiceFee = o.Items.Count == 0 ? 0 : 2;
        o.Discount = s.PromoTagAr != null ? 10 : 0;
        o.Total = Math.Max(0, o.Subtotal + o.DeliveryFee + o.ServiceFee - o.Discount + o.Tip);
        o.CourierPayout = Math.Round(o.DeliveryFee + 12, 2);
    }
}
