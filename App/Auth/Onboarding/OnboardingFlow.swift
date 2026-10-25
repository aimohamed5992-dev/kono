import SwiftUI

/// Post-registration application for merchant / courier accounts. Collects the
/// full KSA paperwork, submits it, then shows an "under review" confirmation.
struct OnboardingFlow: View {
    let role: AppRole
    var onDone: () -> Void            // proceed into the (limited) experience

    var body: some View {
        switch role {
        case .merchant: MerchantOnboardingFlow(onDone: onDone)
        case .courier:  CourierOnboardingFlow(onDone: onDone)
        case .customer: Color.clear.onAppear(perform: onDone)
        }
    }
}

// MARK: - shared chrome

private struct WizardChrome<Content: View>: View {
    let title: String
    let step: Int
    let total: Int
    var canNext: Bool
    var isLast: Bool
    var submitting: Bool
    var onBack: () -> Void
    var onNext: () -> Void
    var onSkip: () -> Void
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                if step > 0 {
                    Button(action: onBack) {
                        Image(systemName: "chevron.right").font(.system(size: 15, weight: .bold))
                            .foregroundStyle(KonoColor.brown).frame(width: 38, height: 38)
                            .background(KonoColor.surface, in: Circle()).konoShadow(.sm)
                    }
                }
                Spacer()
                Text(title).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                Button("لاحقاً", action: onSkip)
                    .font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            }
            .padding(.horizontal, KonoSpace.screen).padding(.top, KonoSpace.s).padding(.bottom, KonoSpace.m)

            OB.Progress(step: step, total: total)
                .padding(.horizontal, KonoSpace.screen).padding(.bottom, KonoSpace.m)

            ScrollView {
                content()
                    .konoScreenPadding()
                    .padding(.top, KonoSpace.s)
                    .padding(.bottom, KonoSpace.xl)
            }
            .scrollDismissesKeyboard(.interactively)

            KonoButton(title: isLast ? "إرسال الطلب للمراجعة" : "التالي",
                       loading: submitting, enabled: canNext, action: onNext)
                .konoScreenPadding()
                .padding(.vertical, KonoSpace.m)
                .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
        .background(KonoColor.background)
    }
}

/// Shown after a successful submit.
struct OnboardingSubmittedView: View {
    let role: AppRole
    var onContinue: () -> Void
    @State private var appear = false

    var body: some View {
        VStack(spacing: KonoSpace.l) {
            Spacer()
            ZStack {
                Circle().fill(KonoColor.success.opacity(0.15)).frame(width: 110, height: 110)
                Image(systemName: "checkmark.seal.fill").font(.system(size: 48))
                    .foregroundStyle(KonoColor.success)
            }
            .scaleEffect(appear ? 1 : 0.6).opacity(appear ? 1 : 0)

            Text("تم استلام طلبك").font(KonoFont.largeTitle).foregroundStyle(KonoColor.espresso)
            Text("يراجع فريق كونو بياناتك ووثائقك خلال ٢٤–٤٨ ساعة عمل، وستصلك رسالة بالنتيجة. يمكنك الآن تجهيز \(role == .merchant ? "متجرك" : "ملفك") بينما تكتمل المراجعة.")
                .font(KonoFont.body).foregroundStyle(KonoColor.textSecondary)
                .multilineTextAlignment(.center)
                .konoScreenPadding()

            Spacer()
            KonoButton(title: role == .merchant ? "الذهاب إلى لوحة المتجر" : "الذهاب إلى ملف المندوب",
                       action: onContinue)
                .konoScreenPadding()
                .padding(.bottom, KonoSpace.xl)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(KonoColor.background)
        .onAppear {
            withAnimation(KonoMotion.bouncy) { appear = true }
            #if canImport(UIKit)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            #endif
        }
    }
}

// MARK: - Courier wizard

struct CourierOnboardingFlow: View {
    var onDone: () -> Void
    @State private var ob = CourierOnboarding()
    @State private var step = 0
    @State private var submitting = false
    @State private var submitted = false
    @State private var error: String?
    private let total = 5

    private var canNext: Bool {
        switch step {
        case 0: return ob.personalDone
        case 1: return ob.vehicleDone
        case 2: return ob.docsDone
        case 3: return ob.bankDone
        default: return ob.canSubmit
        }
    }

    var body: some View {
        if submitted {
            OnboardingSubmittedView(role: .courier, onContinue: onDone)
        } else {
            WizardChrome(title: "تسجيل مندوب", step: step, total: total,
                         canNext: canNext, isLast: step == total - 1, submitting: submitting,
                         onBack: { step -= 1 }, onNext: next, onSkip: onDone) {
                switch step {
                case 0: personal
                case 1: vehicle
                case 2: docs
                case 3: bank
                default: review
                }
            }
            .animation(KonoMotion.snappy, value: step)
            .alert("تعذّر إرسال الطلب", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("حسناً", role: .cancel) {}
            } message: { Text(error ?? "") }
        }
    }

    private func next() {
        if step < total - 1 { step += 1; return }
        submitting = true
        Task {
            defer { submitting = false }
            if await AppData.shared.submitCourierOnboarding(ob) { submitted = true }
            else { error = "تحقّق من اتصالك وحاول مرة أخرى." }
        }
    }

    // steps
    private var personal: some View {
        OB.Section(title: "البيانات الشخصية", subtitle: "كما هي في الهوية / الإقامة") {
            OB.Field(label: "الاسم الكامل", text: $ob.fullNameAr, required: true, icon: "person")
            OB.Field(label: "رقم الهوية / الإقامة", text: $ob.nationalId, required: true,
                     icon: "person.text.rectangle", keyboard: .numberPad, hint: "١٠ أرقام")
            OB.DateField(label: "تاريخ الميلاد", value: $ob.dateOfBirth, required: true)
            OB.Picker(label: "الجنسية", selection: $ob.nationality,
                      options: CourierOnboarding.nationalities.map { ($0, $0) }, required: true)
            OB.Picker(label: "الجنس", selection: $ob.gender, options: [("ذكر", "ذكر"), ("أنثى", "أنثى")])
            OB.Field(label: "البريد الإلكتروني", text: $ob.email, icon: "envelope", keyboard: .emailAddress)
            OB.Picker(label: "المدينة", selection: $ob.cityAr, options: saudiCities.map { ($0, $0) }, required: true)
            OB.Field(label: "العنوان الوطني (الرمز المختصر)", text: $ob.nationalAddress, icon: "mappin")
        }
    }

    private var vehicle: some View {
        OB.Section(title: "بيانات المركبة", subtitle: "وسيلة التوصيل التي ستستخدمها") {
            OB.Picker(label: "نوع وسيلة التوصيل", selection: $ob.vehicleTypeAr,
                      options: CourierOnboarding.vehicleTypes.map { ($0, $0) }, required: true)
            if ob.vehicleTypeAr != "سيراً على الأقدام" {
                OB.Field(label: "الشركة المصنّعة", text: $ob.vehicleMake, required: true,
                         icon: "wrench.and.screwdriver", hint: "هوندا، ياماها، هيونداي…")
                OB.Field(label: "الطراز", text: $ob.vehicleModel, required: true)
                OB.Field(label: "سنة الصنع", text: $ob.vehicleYear, icon: "calendar", keyboard: .numberPad)
                OB.Field(label: "اللون", text: $ob.vehicleColorAr)
                OB.Field(label: "رقم اللوحة", text: $ob.plateAr, required: true,
                         icon: "number", hint: "أ ب ج ١٢٣٤")
                OB.Picker(label: "نوع اللوحة", selection: $ob.plateType,
                          options: CourierOnboarding.plateTypes.map { ($0, $0) })
            }
            OB.Field(label: "رقم تصريح توصيل الطلبات (الهيئة العامة للنقل)", text: $ob.workPermitNumber,
                     icon: "checkmark.shield")
            if ob.vehicleTypeAr != "سيراً على الأقدام" {
                OB.Field(label: "رقم رخصة القيادة", text: $ob.licenseNumber, icon: "creditcard")
                OB.DateField(label: "تاريخ انتهاء الرخصة", value: $ob.licenseExpiry, kind: .expiry)
            }
        }
    }

    private var docs: some View {
        OB.Section(title: "الوثائق المطلوبة", subtitle: "صوّر كل وثيقة بوضوح — يقبل النظام JPG/PNG") {
            VStack(spacing: 0) {
                ForEach($ob.documents) { $d in
                    if !(d.type == "vehicle_reg" || d.type == "vehicle_insurance" || d.type == "driving_license")
                        || ob.vehicleTypeAr != "سيراً على الأقدام" {
                        OB.DocRow(doc: $d)
                        if d.id != ob.documents.last?.id { Divider() }
                    }
                }
            }
            .padding(.horizontal, KonoSpace.m)
            .background(KonoColor.surface).konoCardShape(KonoRadius.m)
        }
    }

    private var bank: some View {
        OB.Section(title: "الحساب البنكي", subtitle: "لإيداع أرباحك أسبوعياً — يجب أن يطابق اسم صاحب الحساب هويتك") {
            OB.Field(label: "رقم الآيبان (IBAN)", text: $ob.iban, required: true,
                     icon: "building.columns", hint: "SA................")
            OB.Picker(label: "اسم البنك", selection: $ob.bankNameAr,
                      options: CourierOnboarding.banks.map { ($0, $0) }, required: true)
            OB.Field(label: "اسم صاحب الحساب", text: $ob.accountHolderAr, required: true, icon: "person.crop.circle")
        }
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            OB.Section(title: "مراجعة وإرسال") {
                summaryRow("الاسم", ob.fullNameAr)
                summaryRow("الهوية", ob.nationalId)
                summaryRow("وسيلة التوصيل", ob.vehicleTypeAr)
                if ob.vehicleTypeAr != "سيراً على الأقدام" {
                    summaryRow("المركبة", "\(ob.vehicleMake) \(ob.vehicleModel) · \(ob.plateAr)")
                }
                summaryRow("الوثائق المرفقة", "\(ob.documents.filter(\.provided).count) من \(ob.documents.count)")
                summaryRow("البنك", ob.bankNameAr)
            }
            OB.Consent(agreed: $ob.agreeTerms)
        }
    }

    private func summaryRow(_ k: String, _ v: String) -> some View {
        HStack(alignment: .top) {
            Text(k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            Spacer()
            Text(v.isBlank ? "—" : v).font(KonoFont.callout).foregroundStyle(KonoColor.textPrimary)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 5)
    }
}

// MARK: - Merchant wizard

struct MerchantOnboardingFlow: View {
    var onDone: () -> Void
    @State private var ob = MerchantOnboarding()
    @State private var step = 0
    @State private var submitting = false
    @State private var submitted = false
    @State private var error: String?
    private let total = 6

    private var canNext: Bool {
        switch step {
        case 0: return ob.businessDone
        case 1: return ob.locationDone
        case 2: return ob.docsDone
        case 3: return ob.bankDone
        case 4: return ob.contactDone
        default: return ob.canSubmit
        }
    }

    var body: some View {
        if submitted {
            OnboardingSubmittedView(role: .merchant, onContinue: onDone)
        } else {
            WizardChrome(title: "تسجيل متجر", step: step, total: total,
                         canNext: canNext, isLast: step == total - 1, submitting: submitting,
                         onBack: { step -= 1 }, onNext: next, onSkip: onDone) {
                switch step {
                case 0: business
                case 1: location
                case 2: docs
                case 3: bank
                case 4: contact
                default: review
                }
            }
            .animation(KonoMotion.snappy, value: step)
            .alert("تعذّر إرسال الطلب", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("حسناً", role: .cancel) {}
            } message: { Text(error ?? "") }
        }
    }

    private func next() {
        if step < total - 1 { step += 1; return }
        submitting = true
        Task {
            defer { submitting = false }
            if await AppData.shared.submitMerchantOnboarding(ob) { submitted = true }
            else { error = "تحقّق من اتصالك وحاول مرة أخرى." }
        }
    }

    private var business: some View {
        OB.Section(title: "بيانات المنشأة", subtitle: "كما هي في السجل التجاري") {
            OB.Field(label: "الاسم التجاري / اسم العلامة", text: $ob.tradeNameAr, required: true, icon: "storefront")
            OB.Picker(label: "نوع النشاط", selection: $ob.activityKind,
                      options: MerchantOnboarding.activities.map { ($0.key, $0.ar) }, required: true)
            OB.Field(label: "وصف مختصر / التصنيف", text: $ob.cuisineAr, icon: "tag", hint: "برجر · أمريكي")
            OB.Field(label: "رقم السجل التجاري", text: $ob.crNumber, required: true,
                     icon: "doc.text", keyboard: .numberPad, hint: "١٠ أرقام")
            OB.DateField(label: "تاريخ انتهاء السجل التجاري", value: $ob.crExpiry, required: true, kind: .expiry)
            OB.Field(label: "الرقم الضريبي (VAT)", text: $ob.vatNumber, icon: "percent",
                     keyboard: .numberPad, hint: "١٥ رقم — إن وُجد")
            OB.Field(label: "نبذة عن المتجر", text: $ob.descriptionAr, icon: "text.alignright")
        }
    }

    private var location: some View {
        OB.Section(title: "الفرع والموقع") {
            OB.Picker(label: "المدينة", selection: $ob.cityAr, options: saudiCities.map { ($0, $0) }, required: true)
            OB.Field(label: "الحي", text: $ob.districtAr, required: true, icon: "map")
            OB.Field(label: "الشارع / العنوان التفصيلي", text: $ob.streetAr, required: true, icon: "signpost.right")
            OB.Field(label: "العنوان الوطني", text: $ob.nationalAddress, icon: "mappin")
            OB.Field(label: "رقم هاتف الفرع", text: $ob.branchPhone, required: true,
                     icon: "phone", keyboard: .phonePad)
            OB.Field(label: "ساعات العمل", text: $ob.openingHoursAr, required: true, icon: "clock")
            OB.MapPicker(label: "الموقع على الخريطة", required: true,
                         latitude: $ob.latitude, longitude: $ob.longitude)
        }
    }

    private var docs: some View {
        OB.Section(title: "الوثائق والتراخيص",
                   subtitle: ob.activityKind == "Pharmacy"
                     ? "الصيدليات تتطلب ترخيص هيئة الغذاء والدواء"
                     : "صوّر كل وثيقة بوضوح") {
            VStack(spacing: 0) {
                let visible = ob.documents.filter {
                    !($0.type == "sfda_license" && ob.activityKind != "Pharmacy")
                }
                ForEach(visible) { d in
                    if let idx = ob.documents.firstIndex(where: { $0.id == d.id }) {
                        OB.DocRow(doc: $ob.documents[idx])
                        if d.id != visible.last?.id { Divider() }
                    }
                }
            }
            .padding(.horizontal, KonoSpace.m)
            .background(KonoColor.surface).konoCardShape(KonoRadius.m)
        }
    }

    private var bank: some View {
        OB.Section(title: "الشعار والحساب البنكي") {
            OB.ImageCard(title: "شعار المتجر", required: true, dataUri: $ob.logoDataUri)
            OB.Field(label: "رقم الآيبان (IBAN)", text: $ob.iban, required: true,
                     icon: "building.columns", hint: "SA................")
            OB.Picker(label: "اسم البنك", selection: $ob.bankNameAr,
                      options: MerchantOnboarding.banks.map { ($0, $0) }, required: true)
            OB.Field(label: "اسم صاحب الحساب (يطابق السجل التجاري)", text: $ob.accountHolderAr,
                     required: true, icon: "person.crop.circle")
        }
    }

    private var contact: some View {
        OB.Section(title: "الإعدادات وجهة الاتصال") {
            OB.Picker(label: "من يقوم بالتوصيل", selection: $ob.deliveryBy,
                      options: MerchantOnboarding.deliveryOptions.map { ($0, "مندوبو \($0)") })
            OB.Field(label: "اسم مسؤول التواصل", text: $ob.contactNameAr, required: true, icon: "person")
            OB.Field(label: "جوال مسؤول التواصل", text: $ob.contactPhone, required: true,
                     icon: "phone", keyboard: .phonePad)
            OB.Field(label: "بريد مسؤول التواصل", text: $ob.contactEmail, icon: "envelope", keyboard: .emailAddress)
        }
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            OB.Section(title: "مراجعة وإرسال") {
                summaryRow("المتجر", ob.tradeNameAr)
                summaryRow("النشاط", MerchantOnboarding.activities.first { $0.key == ob.activityKind }?.ar ?? ob.activityKind)
                summaryRow("السجل التجاري", ob.crNumber)
                summaryRow("الموقع", "\(ob.districtAr)، \(ob.cityAr)")
                summaryRow("الوثائق المرفقة", "\(ob.documents.filter(\.provided).count) من \(ob.documents.count)")
                summaryRow("البنك", ob.bankNameAr)
            }
            OB.Consent(agreed: $ob.agreeTerms)
        }
    }

    private func summaryRow(_ k: String, _ v: String) -> some View {
        HStack(alignment: .top) {
            Text(k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            Spacer()
            Text(v.isBlank ? "—" : v).font(KonoFont.callout).foregroundStyle(KonoColor.textPrimary)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 5)
    }
}

let saudiCities = ["الرياض", "جدة", "مكة المكرمة", "المدينة المنورة", "الدمام", "الخبر", "الأحساء",
                   "الطائف", "بريدة", "تبوك", "خميس مشيط", "أبها", "حائل", "نجران", "الجبيل", "ينبع"]
