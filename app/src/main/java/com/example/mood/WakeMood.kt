package com.example.mood

enum class WakeMood(
    val description: String,
    val emoji: String,
    val colorHex: Long,
    val darkColorHex: Long,
    val energyLevel: Int,      // 0-100
    val stressLevel: Int,      // 0-100
    val positivityLevel: Int,  // 0-100
    val focusLevel: Int,       // 0-100
    val socialLevel: Int,      // 0-100
    val recommendedActivities: List<String>
) {
    ENERGETIC_READY(
        description = "Enerjik & Hazır",
        emoji = "💪",
        colorHex = 0xFFFF6B35,
        darkColorHex = 0xFFFF8C5A,
        energyLevel = 95,
        stressLevel = 15,
        positivityLevel = 90,
        focusLevel = 80,
        socialLevel = 75,
        recommendedActivities = listOf("Yoğun egzersiz", "Zorlu projeler", "Öğrenme", "Liderlik görevleri")
    ),
    MOTIVATED_FOCUSED(
        description = "Motive & Odaklanmış",
        emoji = "🎯",
        colorHex = 0xFF4CAF50,
        darkColorHex = 0xFF66BB6A,
        energyLevel = 80,
        stressLevel = 25,
        positivityLevel = 85,
        focusLevel = 95,
        socialLevel = 50,
        recommendedActivities = listOf("Derin çalışma", "Proje geliştirme", "Analitik görevler", "Planlama")
    ),
    CALM_PEACEFUL(
        description = "Sakin & Huzurlu",
        emoji = "🧘",
        colorHex = 0xFF26A69A,
        darkColorHex = 0xFF4DB6AC,
        energyLevel = 55,
        stressLevel = 10,
        positivityLevel = 80,
        focusLevel = 70,
        socialLevel = 60,
        recommendedActivities = listOf("Meditasyon", "Okuma", "Yaratıcı yazarlık", "Yürüyüş")
    ),
    HAPPY_OPTIMISTIC(
        description = "Mutlu & İyimser",
        emoji = "😊",
        colorHex = 0xFFFFC107,
        darkColorHex = 0xFFFFD54F,
        energyLevel = 75,
        stressLevel = 15,
        positivityLevel = 95,
        focusLevel = 65,
        socialLevel = 90,
        recommendedActivities = listOf("Sosyal etkinlikler", "Takım çalışması", "Yaratıcı projeler", "Mentorluk")
    ),
    NEUTRAL(
        description = "Dengeli & Normal",
        emoji = "😐",
        colorHex = 0xFF78909C,
        darkColorHex = 0xFF90A4AE,
        energyLevel = 50,
        stressLevel = 35,
        positivityLevel = 55,
        focusLevel = 55,
        socialLevel = 50,
        recommendedActivities = listOf("Rutin görevler", "Organizasyon", "Hafif egzersiz", "Planlama")
    ),
    REFLECTIVE_THOUGHTFUL(
        description = "Düşünceli & Derin",
        emoji = "🤔",
        colorHex = 0xFF7E57C2,
        darkColorHex = 0xFF9575CD,
        energyLevel = 45,
        stressLevel = 30,
        positivityLevel = 60,
        focusLevel = 85,
        socialLevel = 30,
        recommendedActivities = listOf("Günlük yazma", "Strateji planlama", "Analiz", "Araştırma")
    ),
    ANXIOUS_WORRIED(
        description = "Endişeli & Kaygılı",
        emoji = "😰",
        colorHex = 0xFFFF7043,
        darkColorHex = 0xFFFF8A65,
        energyLevel = 60,
        stressLevel = 80,
        positivityLevel = 30,
        focusLevel = 40,
        socialLevel = 35,
        recommendedActivities = listOf("Nefes egzersizleri", "Kısa yürüyüş", "Basit görevler", "Müzik dinleme")
    ),
    IRRITABLE_FRUSTRATED(
        description = "Sinirli & Huzursuz",
        emoji = "😤",
        colorHex = 0xFFEF5350,
        darkColorHex = 0xFFE57373,
        energyLevel = 65,
        stressLevel = 85,
        positivityLevel = 20,
        focusLevel = 35,
        socialLevel = 20,
        recommendedActivities = listOf("Fiziksel aktivite", "Meditasyon", "İzolasyon ve dinlenme", "Doğada zaman")
    ),
    SAD_MELANCHOLIC(
        description = "Üzgün & Melankolik",
        emoji = "😢",
        colorHex = 0xFF5C6BC0,
        darkColorHex = 0xFF7986CB,
        energyLevel = 25,
        stressLevel = 50,
        positivityLevel = 15,
        focusLevel = 30,
        socialLevel = 25,
        recommendedActivities = listOf("Kendine bakım", "Sevdiklerinle konuşma", "Sıcak içecek", "Hafif müzik")
    ),
    EXHAUSTED_BURNOUT(
        description = "Tükenmiş & Bitkin",
        emoji = "🫠",
        colorHex = 0xFF8D6E63,
        darkColorHex = 0xFFA1887F,
        energyLevel = 10,
        stressLevel = 70,
        positivityLevel = 20,
        focusLevel = 15,
        socialLevel = 15,
        recommendedActivities = listOf("Dinlenme", "Uyku", "Basit yürüyüş", "Dijital detoks")
    ),
    SLEEPY_TIRED(
        description = "Uykulu & Yorgun",
        emoji = "😴",
        colorHex = 0xFF42A5F5,
        darkColorHex = 0xFF64B5F6,
        energyLevel = 15,
        stressLevel = 30,
        positivityLevel = 40,
        focusLevel = 20,
        socialLevel = 30,
        recommendedActivities = listOf("Yavaş başla", "Kahve/çay", "Hafif esneme", "Kolay görevler")
    ),
    CREATIVE_INSPIRED(
        description = "Yaratıcı & İlhamlı",
        emoji = "✨",
        colorHex = 0xFFAB47BC,
        darkColorHex = 0xFFCE93D8,
        energyLevel = 70,
        stressLevel = 20,
        positivityLevel = 85,
        focusLevel = 75,
        socialLevel = 55,
        recommendedActivities = listOf("Sanat", "Beyin fırtınası", "Yeni fikirler", "Deneysel çalışma")
    );

    companion object {
        fun fromDimensions(
            energy: Float,
            stress: Float,
            positivity: Float,
            focus: Float,
            social: Float
        ): WakeMood {
            // Find closest mood by euclidean distance in 5D space
            return entries.minByOrNull { mood ->
                val dE = (mood.energyLevel - energy)
                val dS = (mood.stressLevel - stress)
                val dP = (mood.positivityLevel - positivity)
                val dF = (mood.focusLevel - focus)
                val dSo = (mood.socialLevel - social)
                dE * dE + dS * dS + dP * dP + dF * dF + dSo * dSo
            } ?: NEUTRAL
        }
    }
}