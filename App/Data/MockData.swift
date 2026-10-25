import SwiftUI

/// All sample content for the prototype lives here. Arabic-first, Saudi context:
/// real-sounding brands, SAR pricing, Riyadh districts.
enum Mock {

    // MARK: Promo slides

    static let promos: [PromoSlide] = [
        PromoSlide(titleAr: "توصيل مجاني", subtitleAr: "على أول ٣ طلبات من المطاعم المشاركة",
                   ctaAr: "اطلب الآن", illustration: "promo_free_delivery", bg: Color(hex: 0xFFE79A)),
        PromoSlide(titleAr: "اشترك في كونو بلس", subtitleAr: "ووفّر رسوم التوصيل طوال الشهر",
                   ctaAr: "اشترك الآن", illustration: "promo_konoplus", bg: Color(hex: 0xFFEFC2)),
        PromoSlide(titleAr: "خصم ٣٠٪ على المقاضي", subtitleAr: "من بندة ولولو — حتى نهاية الأسبوع",
                   ctaAr: "تسوّق الآن", illustration: "promo_grocery_sale", bg: Color(hex: 0xE9F5D8)),
    ]

    // MARK: Verticals

    static let verticals: [Vertical] = [
        Vertical(kind: .restaurants, etaMinutes: 25, storeCount: 1240),
        Vertical(kind: .groceries,   etaMinutes: 35, storeCount: 86),
        Vertical(kind: .pharmacy,    etaMinutes: 20, storeCount: 54),
        Vertical(kind: .sweets,      etaMinutes: 40, storeCount: 130),
        Vertical(kind: .flowers,     etaMinutes: 45, storeCount: 38),
        Vertical(kind: .water,       etaMinutes: 60, storeCount: 12),
    ]

    // MARK: Addresses

    static let addresses: [Address] = [
        Address(labelAr: "المنزل", lineAr: "حي الياسمين، شارع الأمير سلطان", cityAr: "الرياض",
                icon: "house.fill", latitude: 24.813, longitude: 46.637, isDefault: true),
        Address(labelAr: "العمل", lineAr: "طريق الملك فهد، برج المملكة", cityAr: "الرياض",
                icon: "briefcase.fill", latitude: 24.711, longitude: 46.674),
        Address(labelAr: "بيت الوالدة", lineAr: "حي النرجس، شارع أنس بن مالك", cityAr: "الرياض",
                icon: "heart.fill", latitude: 24.842, longitude: 46.651),
    ]

    // MARK: Payment methods

    static let payments: [PaymentMethod] = [
        PaymentMethod(labelAr: "Apple Pay", icon: "apple.logo", detailAr: nil, isDefault: true),
        PaymentMethod(labelAr: "مدى", icon: "creditcard.fill", detailAr: "**** 4421"),
        PaymentMethod(labelAr: "محفظة كونو", icon: "wallet.pass.fill", detailAr: "84.00 ر.س"),
        PaymentMethod(labelAr: "الدفع عند الاستلام", icon: "banknote.fill", detailAr: "نقداً"),
    ]

    // MARK: Couriers

    static let courier = Courier(
        name: "سعود الحربي", rating: 4.9, trips: 2140, vehicleAr: "دباب",
        plateAr: "ن ح ر · ٤٨٢١", photoAsset: "courier_1",
        latitude: 24.802, longitude: 46.641
    )

    // MARK: Stores

    static let stores: [Store] = [
        Store(name: "برجرايزر", kind: .restaurants, cuisineAr: "برجر · أمريكي",
              rating: 4.7, ratingCount: 3120, etaMinutes: 25, deliveryFee: 0, minOrder: 25,
              distanceKm: 1.8, promoTagAr: "توصيل مجاني", heroAsset: "store_burger",
              logoAsset: "logo_burger", tagsAr: ["الأكثر طلباً", "برجر لحم"], priceLevel: 2),
        Store(name: "شاورما هوس", kind: .restaurants, cuisineAr: "شاورما · لبناني",
              rating: 4.5, ratingCount: 1890, etaMinutes: 30, deliveryFee: 5, minOrder: 20,
              distanceKm: 2.6, promoTagAr: "خصم ٢٥٪", heroAsset: "store_shawarma",
              logoAsset: "logo_shawarma", tagsAr: ["شاورما دجاج"], priceLevel: 1),
        Store(name: "بيتزا فورنو", kind: .restaurants, cuisineAr: "بيتزا · إيطالي",
              rating: 4.6, ratingCount: 2210, etaMinutes: 35, deliveryFee: 8, minOrder: 40,
              distanceKm: 3.9, promoTagAr: nil, heroAsset: "store_pizza",
              logoAsset: "logo_pizza", tagsAr: ["عجينة طازجة"], priceLevel: 3),
        Store(name: "كوفي رومرز", kind: .restaurants, cuisineAr: "قهوة مختصة · حلا",
              rating: 4.8, ratingCount: 980, etaMinutes: 20, deliveryFee: 6, minOrder: 15,
              distanceKm: 1.2, isBusy: true, promoTagAr: nil, heroAsset: "store_coffee",
              logoAsset: "logo_coffee", tagsAr: ["مختص"], priceLevel: 2),
        Store(name: "بندة", kind: .groceries, cuisineAr: "سوبرماركت",
              rating: 4.4, ratingCount: 5400, etaMinutes: 40, deliveryFee: 10, minOrder: 50,
              distanceKm: 4.5, promoTagAr: "خصم على أول طلب", heroAsset: "store_panda",
              logoAsset: "logo_panda", tagsAr: ["مقاضي البيت"], priceLevel: 2),
        Store(name: "لولو هايبر ماركت", kind: .groceries, cuisineAr: "هايبر ماركت",
              rating: 4.5, ratingCount: 4100, etaMinutes: 45, deliveryFee: 12, minOrder: 60,
              distanceKm: 6.1, promoTagAr: nil, heroAsset: "store_lulu",
              logoAsset: "logo_lulu", tagsAr: ["تشكيلة واسعة"], priceLevel: 2),
        Store(name: "التميمي ماركت", kind: .groceries, cuisineAr: "سوبرماركت",
              rating: 4.3, ratingCount: 2600, etaMinutes: 38, deliveryFee: 9, minOrder: 45,
              distanceKm: 3.4, promoTagAr: nil, heroAsset: "store_tamimi",
              logoAsset: "logo_tamimi", tagsAr: [], priceLevel: 2),
        Store(name: "صيدلية النهدي", kind: .pharmacy, cuisineAr: "صيدلية",
              rating: 4.7, ratingCount: 3300, etaMinutes: 18, deliveryFee: 7, minOrder: 0,
              distanceKm: 1.1, promoTagAr: "توصيل خلال ٢٠ دقيقة", heroAsset: "store_nahdi",
              logoAsset: "logo_nahdi", tagsAr: ["دواء", "عناية"], priceLevel: 2),
        Store(name: "صيدلية الدواء", kind: .pharmacy, cuisineAr: "صيدلية",
              rating: 4.6, ratingCount: 2100, etaMinutes: 22, deliveryFee: 7, minOrder: 0,
              distanceKm: 2.0, promoTagAr: nil, heroAsset: "store_dawaa",
              logoAsset: "logo_dawaa", tagsAr: ["عناية بالطفل"], priceLevel: 2),
        Store(name: "حلويات سعد الدين", kind: .sweets, cuisineAr: "حلويات شرقية",
              rating: 4.6, ratingCount: 1500, etaMinutes: 40, deliveryFee: 10, minOrder: 30,
              distanceKm: 5.0, promoTagAr: nil, heroAsset: "store_sweets",
              logoAsset: "logo_sweets", tagsAr: ["كنافة", "بقلاوة"], priceLevel: 2),
        Store(name: "دجاج تكساس", kind: .restaurants, cuisineAr: "دجاج مقلي · أمريكي",
              rating: 4.4, ratingCount: 2740, etaMinutes: 28, deliveryFee: 0, minOrder: 30,
              distanceKm: 2.2, promoTagAr: "توصيل مجاني", heroAsset: "store_shawarma",
              logoAsset: "", tagsAr: ["دجاج مقرمش"], priceLevel: 2),
        Store(name: "بيك", kind: .restaurants, cuisineAr: "وجبات سريعة · سعودي",
              rating: 4.9, ratingCount: 12800, etaMinutes: 22, deliveryFee: 4, minOrder: 15,
              distanceKm: 0.9, promoTagAr: nil, heroAsset: "store_burger",
              logoAsset: "", tagsAr: ["الأكثر طلباً"], priceLevel: 1),
        Store(name: "كافيه أرابيكا", kind: .restaurants, cuisineAr: "قهوة · مخبوزات",
              rating: 4.7, ratingCount: 640, etaMinutes: 18, deliveryFee: 6, minOrder: 20,
              distanceKm: 1.5, promoTagAr: "خصم ٢٠٪", heroAsset: "store_coffee",
              logoAsset: "", tagsAr: ["كرواسون"], priceLevel: 2),
        Store(name: "كارفور", kind: .groceries, cuisineAr: "هايبر ماركت",
              rating: 4.4, ratingCount: 3900, etaMinutes: 50, deliveryFee: 12, minOrder: 70,
              distanceKm: 7.0, promoTagAr: "خصم ٣٠٪ اليوم", heroAsset: "store_lulu",
              logoAsset: "", tagsAr: [], priceLevel: 2),
        Store(name: "صيدلية المتحدة", kind: .pharmacy, cuisineAr: "صيدلية",
              rating: 4.5, ratingCount: 1200, etaMinutes: 25, deliveryFee: 5, minOrder: 0,
              distanceKm: 2.8, promoTagAr: nil, heroAsset: "store_nahdi",
              logoAsset: "", tagsAr: ["فيتامينات"], priceLevel: 2),
        Store(name: "بيت الكيك", kind: .sweets, cuisineAr: "كيك ومناسبات",
              rating: 4.8, ratingCount: 890, etaMinutes: 45, deliveryFee: 15, minOrder: 50,
              distanceKm: 4.2, promoTagAr: nil, heroAsset: "store_sweets",
              logoAsset: "", tagsAr: ["كيك مناسبات"], priceLevel: 3),
    ]

    static func stores(for kind: VerticalKind) -> [Store] {
        stores.filter { $0.kind == kind }
    }

    // MARK: Menus

    static func menu(for store: Store) -> [MenuCategory] {
        switch store.kind {
        case .restaurants: return restaurantMenu
        case .groceries:   return groceryCatalog
        case .pharmacy:    return pharmacyCatalog
        default:           return restaurantMenu
        }
    }

    static let restaurantMenu: [MenuCategory] = [
        MenuCategory(nameAr: "الأكثر طلباً", items: [
            Product(nameAr: "برجر كلاسيك مزدوج", descAr: "قطعتا لحم أنجوس، جبن شيدر، صوص كونو الخاص",
                    price: 32, oldPrice: 39, imageAsset: "food_burger_double", isPopular: true,
                    calories: 780, options: burgerOptions),
            Product(nameAr: "دجاج مقرمش سبايسي", descAr: "صدر دجاج مقرمش، مخلل، صوص حار",
                    price: 27, oldPrice: nil, imageAsset: "food_chicken", isPopular: true, calories: 640),
            Product(nameAr: "بطاطس ودجز بالجبن", descAr: "بطاطس مقلية مع صوص الجبن والهالبينو",
                    price: 15, oldPrice: nil, imageAsset: "food_fries", calories: 420),
        ]),
        MenuCategory(nameAr: "برجر", items: [
            Product(nameAr: "برجر مشروم و سويس", descAr: "لحم أنجوس، مشروم سوتيه، جبن سويسري",
                    price: 34, oldPrice: nil, imageAsset: "food_burger_mushroom", calories: 720,
                    options: burgerOptions),
            Product(nameAr: "برجر باربكيو", descAr: "لحم، بصل مكرمل، صوص باربكيو، حلقات بصل",
                    price: 36, oldPrice: nil, imageAsset: "food_burger_bbq", calories: 810,
                    options: burgerOptions),
        ]),
        MenuCategory(nameAr: "مشروبات", items: [
            Product(nameAr: "ميلك شيك فانيلا", descAr: "حليب طازج، آيس كريم فانيلا، كريمة",
                    price: 18, oldPrice: nil, imageAsset: "food_shake", calories: 390),
            Product(nameAr: "مشروب غازي", descAr: "كولا / سبرايت / فانتا", price: 6,
                    oldPrice: nil, imageAsset: "food_soda"),
        ]),
    ]

    static let groceryCatalog: [MenuCategory] = [
        MenuCategory(nameAr: "عروض اليوم", items: [
            Product(nameAr: "حليب المراعي كامل الدسم", descAr: "عبوة ٢ لتر",
                    price: 9.5, oldPrice: 12, imageAsset: "grocery_milk", isPopular: true,
                    unitAr: "٢ لتر"),
            Product(nameAr: "بيض المزرعة طازج", descAr: "كبير الحجم", price: 14, oldPrice: 17,
                    imageAsset: "grocery_eggs", unitAr: "٣٠ حبة"),
            Product(nameAr: "موز إكوادوري", descAr: "طازج", price: 7.9, oldPrice: nil,
                    imageAsset: "grocery_banana", unitAr: "١ كجم"),
        ]),
        MenuCategory(nameAr: "خضار وفواكه", items: [
            Product(nameAr: "طماطم", descAr: "درجة أولى", price: 5.5, oldPrice: nil,
                    imageAsset: "grocery_tomato", unitAr: "١ كجم"),
            Product(nameAr: "تفاح أحمر", descAr: "مستورد", price: 11, oldPrice: nil,
                    imageAsset: "grocery_apple", unitAr: "١ كجم"),
        ]),
        MenuCategory(nameAr: "معلبات وأساسيات", items: [
            Product(nameAr: "أرز بسمتي أبو كاس", descAr: "حبة طويلة", price: 42, oldPrice: 49,
                    imageAsset: "grocery_rice", unitAr: "٥ كجم"),
            Product(nameAr: "زيت زيتون بكر ممتاز", descAr: "معصور على البارد", price: 38,
                    oldPrice: nil, imageAsset: "grocery_oil", unitAr: "٧٥٠ مل"),
        ]),
    ]

    static let pharmacyCatalog: [MenuCategory] = [
        MenuCategory(nameAr: "الأكثر طلباً", items: [
            Product(nameAr: "بنادول أدفانس", descAr: "مسكن وخافض حرارة - ٢٤ قرص",
                    price: 21, oldPrice: nil, imageAsset: "pharma_panadol", isPopular: true,
                    unitAr: "٢٤ قرص"),
            Product(nameAr: "فيتامين سي ١٠٠٠", descAr: "فوار - ٢٠ قرص", price: 33, oldPrice: 39,
                    imageAsset: "pharma_vitc", unitAr: "٢٠ قرص"),
        ]),
        MenuCategory(nameAr: "العناية والطفل", items: [
            Product(nameAr: "حفاضات بامبرز مقاس ٤", descAr: "عبوة كبيرة", price: 72, oldPrice: 85,
                    imageAsset: "pharma_diapers", unitAr: "٧٦ حفاضة"),
            Product(nameAr: "غسول سيتافيل", descAr: "للبشرة الحساسة", price: 55, oldPrice: nil,
                    imageAsset: "pharma_cetaphil", unitAr: "٤٧٣ مل"),
        ]),
    ]

    static let burgerOptions: [ProductOptionGroup] = [
        ProductOptionGroup(titleAr: "الحجم", required: true, multiSelect: false, choices: [
            ProductOption(nameAr: "عادي"),
            ProductOption(nameAr: "وجبة (بطاطس + مشروب)", priceDelta: 12),
        ]),
        ProductOptionGroup(titleAr: "إضافات", required: false, multiSelect: true, choices: [
            ProductOption(nameAr: "جبن إضافي", priceDelta: 4),
            ProductOption(nameAr: "بيكون", priceDelta: 6),
            ProductOption(nameAr: "صوص حار", priceDelta: 2),
        ]),
        ProductOptionGroup(titleAr: "بدون", required: false, multiSelect: true, choices: [
            ProductOption(nameAr: "بدون بصل"),
            ProductOption(nameAr: "بدون مخلل"),
        ]),
    ]

    // MARK: Cart / active order

    static var sampleCart: [CartLine] {
        [
            CartLine(product: restaurantMenu[0].items[0], quantity: 1,
                     selectedOptionsAr: ["وجبة (بطاطس + مشروب)", "جبن إضافي"]),
            CartLine(product: restaurantMenu[0].items[1], quantity: 2, selectedOptionsAr: []),
            CartLine(product: restaurantMenu[2].items[0], quantity: 1, selectedOptionsAr: []),
        ]
    }

    static var activeOrder: Order {
        Order(shortCode: "KON-4821", store: stores[0], lines: sampleCart,
              stage: .onTheWay, placedAt: Date().addingTimeInterval(-1500),
              address: addresses[0], courier: courier, paymentLabelAr: "Apple Pay",
              scheduledForAr: nil)
    }

    static var pastOrders: [Order] {
        [
            Order(shortCode: "KON-4790", store: stores[4], lines: Array(sampleCart.prefix(2)),
                  stage: .delivered, placedAt: Date().addingTimeInterval(-86400),
                  address: addresses[0], courier: courier, paymentLabelAr: "مدى **** 4421",
                  scheduledForAr: nil),
            Order(shortCode: "KON-4712", store: stores[7], lines: Array(sampleCart.suffix(1)),
                  stage: .delivered, placedAt: Date().addingTimeInterval(-3 * 86400),
                  address: addresses[1], courier: courier, paymentLabelAr: "Apple Pay",
                  scheduledForAr: nil),
        ]
    }

    // MARK: Customer profile

    static var customer = CustomerProfile(
        nameAr: "أحمد المطيري", phone: "+966 55 123 4567", email: "ahmed@example.com",
        konoPlus: true, walletBalance: 84, points: 1240,
        addresses: addresses, payments: payments
    )

    // MARK: Merchant side

    static var merchant = MerchantProfile(
        storeName: "برجرايزر - فرع الياسمين", kind: .restaurants, isOnline: true,
        rating: 4.7, todayOrders: 38, todayRevenue: 2740, prepMinutes: 15
    )

    static func merchantTickets() -> [MerchantTicket] {
        [
            MerchantTicket(shortCode: "KON-4821", customerNameAr: "أحمد م.",
                           lines: sampleCart, placedAt: Date().addingTimeInterval(-60),
                           state: .incoming, isDelivery: true, courierNameAr: nil,
                           noteAr: "الجرس لا يعمل، اتصل عند الوصول",
                           acceptDeadline: Date().addingTimeInterval(55)),
            MerchantTicket(shortCode: "KON-4820", customerNameAr: "نورة ع.",
                           lines: Array(sampleCart.prefix(2)), placedAt: Date().addingTimeInterval(-360),
                           state: .preparing, isDelivery: true, courierNameAr: "سعود ح.",
                           noteAr: nil, acceptDeadline: Date()),
            MerchantTicket(shortCode: "KON-4818", customerNameAr: "خالد س.",
                           lines: Array(sampleCart.suffix(1)), placedAt: Date().addingTimeInterval(-720),
                           state: .ready, isDelivery: false, courierNameAr: nil,
                           noteAr: nil, acceptDeadline: Date()),
            MerchantTicket(shortCode: "KON-4815", customerNameAr: "سارة ف.",
                           lines: sampleCart, placedAt: Date().addingTimeInterval(-1800),
                           state: .handedOver, isDelivery: true, courierNameAr: "ماجد ر.",
                           noteAr: nil, acceptDeadline: Date()),
        ]
    }

    static func merchantMenuStatus() -> [MenuItemStatus] {
        restaurantMenu.flatMap { $0.items }.enumerated().map { i, p in
            MenuItemStatus(product: p, available: i != 3, soldToday: (7 - i) * 4 + 3)
        }
    }

    // MARK: Courier side

    static var courierProfile = CourierProfile(
        name: "سعود الحربي", rating: 4.9, isOnline: true, vehicleAr: "دباب",
        todayTrips: 12, todayEarnings: 214, todayDistanceKm: 47.5,
        acceptanceRate: 94, photoAsset: "courier_1"
    )

    static func courierOffer() -> CourierJob {
        CourierJob(shortCode: "KON-4821", storeNameAr: "برجرايزر - الياسمين",
                   storeAddressAr: "حي الياسمين، طريق الأمير محمد بن سلمان",
                   dropoffAddressAr: "حي النرجس، شارع أنس بن مالك",
                   customerNameAr: "أحمد م.", distanceKm: 3.4, estMinutes: 14,
                   payout: 18.5, itemCount: 4, isBatched: false, stage: .offered,
                   storeLat: 24.826, storeLng: 46.639, dropLat: 24.842, dropLng: 46.651)
    }

    static func courierEarningsWeek() -> [EarningsDay] {
        [
            EarningsDay(weekdayAr: "السبت", amount: 260, trips: 15),
            EarningsDay(weekdayAr: "الأحد", amount: 190, trips: 11),
            EarningsDay(weekdayAr: "الاثنين", amount: 240, trips: 14),
            EarningsDay(weekdayAr: "الثلاثاء", amount: 175, trips: 10),
            EarningsDay(weekdayAr: "الأربعاء", amount: 300, trips: 18),
            EarningsDay(weekdayAr: "الخميس", amount: 340, trips: 20),
            EarningsDay(weekdayAr: "الجمعة", amount: 214, trips: 12),
        ]
    }
}
