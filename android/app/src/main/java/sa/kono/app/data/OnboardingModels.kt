package sa.kono.app.data

import kotlinx.serialization.Serializable

/** One required/optional document in an onboarding checklist. `provided` flips once the applicant attaches something. */
@Serializable
data class OnboardingDoc(val type: String, val labelAr: String, val required: Boolean, val provided: Boolean = false, val fileName: String = "")

object OnboardingChecklists {
    val courier = listOf(
        OnboardingDoc("national_id", "صورة الهوية / الإقامة (الوجهان)", true),
        OnboardingDoc("driving_license", "رخصة القيادة سارية المفعول", true),
        OnboardingDoc("delivery_permit", "تصريح توصيل الطلبات (الهيئة العامة للنقل)", true),
        OnboardingDoc("vehicle_reg", "استمارة المركبة (رخصة السير)", true),
        OnboardingDoc("vehicle_insurance", "وثيقة تأمين المركبة", true),
        OnboardingDoc("personal_photo", "صورة شخصية حديثة وواضحة", true),
        OnboardingDoc("criminal_record", "شهادة عدم محكومية / السجل الجنائي", false),
        OnboardingDoc("iban_certificate", "شهادة الآيبان", true),
    )
    val merchant = listOf(
        OnboardingDoc("commercial_reg", "السجل التجاري", true),
        OnboardingDoc("municipal_license", "الرخصة البلدية (رخصة المحل)", true),
        OnboardingDoc("civil_defense", "شهادة السلامة — الدفاع المدني", true),
        OnboardingDoc("health_certs", "الشهادات الصحية للعاملين", true),
        OnboardingDoc("sfda_license", "ترخيص هيئة الغذاء والدواء (للصيدليات)", true),
        OnboardingDoc("lease_contract", "عقد الإيجار / صك الموقع", false),
        OnboardingDoc("owner_id", "هوية المالك / المدير المفوّض", true),
        OnboardingDoc("authorization", "تفويض بالتسجيل (إن لزم)", false),
        OnboardingDoc("iban_certificate", "شهادة الآيبان", true),
        OnboardingDoc("store_logo", "شعار المتجر (عالي الدقة)", true),
    )
}

private val String.digits get() = count { it.isDigit() }
private val String.isBlankText get() = isBlank()

/** Courier (مندوب توصيل) application — KSA delivery-rider paperwork. Immutable; edit with `copy`. */
@Serializable
data class CourierOnboarding(
    val fullNameAr: String = "", val nationalId: String = "", val dateOfBirth: String = "",
    val nationality: String = "سعودي", val gender: String = "ذكر", val email: String = "",
    val cityAr: String = "الرياض", val nationalAddress: String = "",
    val vehicleTypeAr: String = "دباب", val vehicleMake: String = "", val vehicleModel: String = "",
    val vehicleYear: Int? = null, val vehicleColorAr: String = "", val plateAr: String = "", val plateType: String = "دراجة نارية",
    val workPermitNumber: String = "", val licenseNumber: String = "", val licenseExpiry: String = "",
    val iban: String = "SA", val bankNameAr: String = "", val accountHolderAr: String = "",
    val latitude: Double? = null, val longitude: Double? = null,
    val documents: List<OnboardingDoc> = OnboardingChecklists.courier, val agreeTerms: Boolean = false,
) {
    val walking get() = vehicleTypeAr == "سيراً على الأقدام"
    val personalDone get() = fullNameAr.isNotBlank() && nationalId.digits == 10 && dateOfBirth.isNotBlank() && nationality.isNotBlank() && cityAr.isNotBlank()
    val vehicleDone get() = walking || (vehicleMake.isNotBlank() && vehicleModel.isNotBlank() && plateAr.isNotBlank())
    val docsDone get() = documents.filter { it.required }.all { it.provided }
    val bankDone get() = iban.digits >= 20 && bankNameAr.isNotBlank() && accountHolderAr.isNotBlank()
    val canSubmit get() = personalDone && vehicleDone && docsDone && bankDone && agreeTerms

    companion object {
        val vehicleTypes = listOf("دباب", "سيارة", "دراجة هوائية", "سيراً على الأقدام")
        val plateTypes = listOf("دراجة نارية", "خصوصي", "نقل عام")
        val nationalities = listOf("سعودي", "مصري", "سوري", "يمني", "باكستاني", "هندي", "بنغلاديشي", "أخرى")
        val banks = listOf("الأهلي", "الراجحي", "الرياض", "البلاد", "الإنماء", "سامبا", "الجزيرة", "ساب", "الفرنسي", "العربي الوطني")
    }
}

/** Merchant (متجر / مطعم) application — KSA store paperwork. */
@Serializable
data class MerchantOnboarding(
    val tradeNameAr: String = "", val activityKind: String = "Restaurants", val cuisineAr: String = "",
    val crNumber: String = "", val crExpiry: String = "", val vatNumber: String = "", val descriptionAr: String = "",
    val cityAr: String = "الرياض", val districtAr: String = "", val streetAr: String = "",
    val latitude: Double? = null, val longitude: Double? = null, val nationalAddress: String = "",
    val openingHoursAr: String = "١٠:٠٠ ص – ١:٠٠ ص", val branchPhone: String = "",
    val iban: String = "SA", val bankNameAr: String = "", val accountHolderAr: String = "",
    val contactNameAr: String = "", val contactPhone: String = "", val contactEmail: String = "",
    val logoDataUri: String? = null, val prepMinutes: Int = 20, val minOrder: Double = 0.0, val deliveryBy: String = "كونو",
    val documents: List<OnboardingDoc> = OnboardingChecklists.merchant, val agreeTerms: Boolean = false,
) {
    val businessDone get() = tradeNameAr.isNotBlank() && crNumber.digits == 10 && crExpiry.isNotBlank() && activityKind.isNotBlank()
    val locationDone get() = cityAr.isNotBlank() && districtAr.isNotBlank() && streetAr.isNotBlank() && latitude != null && branchPhone.isNotBlank() && openingHoursAr.isNotBlank()
    val bankDone get() = iban.digits >= 20 && bankNameAr.isNotBlank() && accountHolderAr.isNotBlank()
    val contactDone get() = contactNameAr.isNotBlank() && contactPhone.isNotBlank()
    val docsDone get() = documents.filter { it.required && !(it.type == "sfda_license" && activityKind != "Pharmacy") }.all { it.provided }
    val canSubmit get() = businessDone && locationDone && bankDone && contactDone && docsDone && agreeTerms

    companion object {
        val activities = listOf("Restaurants" to "مطعم", "Restaurants" to "كافيه", "Sweets" to "حلويات ومخبوزات", "Groceries" to "بقالة / سوبرماركت", "Pharmacy" to "صيدلية", "Flowers" to "ورود وهدايا", "Water" to "مياه")
        val deliveryOptions = listOf("كونو", "المتجر")
    }
}

@Serializable data class OnboardingState(val status: String, val payloadJson: String = "")

val saudiCities = listOf("الرياض", "جدة", "مكة المكرمة", "المدينة المنورة", "الدمام", "الخبر", "الأحساء", "الطائف", "بريدة", "تبوك", "خميس مشيط", "أبها", "حائل", "نجران", "الجبيل", "ينبع")

// ── gateway ──
val AppData.onboardingStatus: String get() = me?.onboardingStatus.orEmpty()
val AppData.needsOnboarding: Boolean get() = role != AppRole.Customer && onboardingStatus.isEmpty()

suspend fun AppData.submitCourierOnboarding(ob: CourierOnboarding): Boolean = try {
    applyOnboarding(KonoApi.post<CourierOnboarding, OnboardingState>("/api/onboarding/courier", ob)); true
} catch (e: ApiError) { false }

suspend fun AppData.submitMerchantOnboarding(ob: MerchantOnboarding): Boolean = try {
    applyOnboarding(KonoApi.post<MerchantOnboarding, OnboardingState>("/api/onboarding/merchant", ob)); true
} catch (e: ApiError) { false }

private fun AppData.applyOnboarding(st: OnboardingState) { me?.let { replaceMe(it.copy(onboardingStatus = st.status)) } }
