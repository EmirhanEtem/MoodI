<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Moodİİ Logo" width="120"/>
</p>

<h1 align="center">Moodİİ — WakeMood Planner</h1>

<p align="center">
  <b>Yapay Zeka Destekli Sabah Ruh Hali Analizörü ve Kişiselleştirilmiş Günlük Planlayıcı</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" alt="Platform"/>
  <img src="https://img.shields.io/badge/Dil-Kotlin-blue?logo=kotlin" alt="Dil"/>
  <img src="https://img.shields.io/badge/Aray%C3%BCz-Jetpack%20Compose-brightgreen?logo=jetpackcompose" alt="Arayüz"/>
  <img src="https://img.shields.io/badge/YZ-Google%20Gemini-orange?logo=google" alt="YZ"/>
  <img src="https://img.shields.io/badge/M%C3%BCzik-Spotify%20API-1DB954?logo=spotify" alt="Spotify"/>
  <img src="https://img.shields.io/badge/ML-MLKit%20Y%C3%BCz%20Alg%C4%B1lama-red?logo=google" alt="ML Kit"/>
  <img src="https://img.shields.io/badge/Min%20SDK-26-yellow" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Hedef%20SDK-34-yellow" alt="Hedef SDK"/>
</p>

---

## 📖 Genel Bakış

**Moodİİ (WakeMood Planner)**, kullanıcının sabahki ruh halini üç farklı giriş kanalıyla analiz eden akıllı bir Android uygulamasıdır: **yüz ifadesi tarama**, **sesli giriş** ve **alarm erteleme sayısı**. Toplanan veriler doğrultusunda **Google Gemini AI** kullanarak kişiselleştirilmiş, uygulanabilir günlük eylem planları oluşturur. Ayrıca **Spotify** entegrasyonu sayesinde ruh haline uygun çalma listeleri oluşturma özelliği sunar.

Uygulama, planlarını kullanıcının **mesleğine** (Öğrenci, Yazılımcı, Doktor, Öğretmen, Sanatçı veya Diğer) ve **özel durumlarına** (yaklaşan sınavlar, proje teslim tarihleri, nöbetler, etkinlikler vb.) göre adapte eder.

---

## ✨ Temel Özellikler

### 🧠 Çoklu Kanal Ruh Hali Analizi
- **Yüz İfadesi Tarama** — CameraX + ML Kit Yüz Algılama ile gerçek zamanlı gülümseme olasılığı ölçümü, yüz kontur ve referans noktası çizimleri
- **Sesli Giriş** — Türkçe dil desteğiyle konuşma tanıma; duygu anahtar kelimelerini ayrıştırma ("harika", "yorgunum", "sinirliyim" vb.)
- **Alarm Erteleme Takibi** — Kullanıcının alarmını kaç kez ertelediği bilgisi ruh hali hesaplamasına dahil edilir
- **Bileşik Ruh Hali Puanlaması** — Üç girdi birleştirilerek ağırlıklı bir puan hesaplanır ve dört ruh hali kategorisinden birine eşlenir

### 🎭 Dört Ruh Hali Kategorisi
| Ruh Hali | Açıklama | Etki |
|----------|----------|------|
| 💪 **ENERJİK** | Yoğun tempolu görevler modu | Yoğun görev planlaması |
| 😴 **UYKULU** | Yavaş başla modu | Nazik ve kademeli görev akışı |
| 🧘 **STRESLİ** | Meditasyon + basit işler modu | Stres azaltma odaklı plan |
| 🙂 **NÖTR** | Standart plan modu | Dengeli günlük plan |

### 👤 Mesleğe Özel Planlama
- **Öğrenci** — Sınav takvimi girişi, Pomodoro/Feynman teknikleriyle çalışma planı, sınav günü tespiti
- **Yazılımcı** — Proje teslim tarihi takibi, kod inceleme blokları, öğrenme seansları
- **Doktor** — Nöbet/vardiya takibi, dinlenme planlaması, hasta hazırlığı
- **Öğretmen** — Ders planı hazırlığı, sınav okuma seansları, veli toplantısı planlaması
- **Sanatçı** — Sergi/etkinlik tarihleri, yaratıcı ilham blokları, portfolyo çalışması

### 🤖 Google Gemini AI Entegrasyonu
- **Gemini 2.0 Flash** modeli ile hızlı ve bağlama duyarlı plan üretimi
- Kullanıcı profili, ruh hali, meslek, saat ve özel bağlamı içeren dinamik prompt oluşturma
- Genişletilebilir görev kartları — tıklama ile AI tarafından üretilen alt görevler
- API erişilemediğinde yedek görev sistemi

### 🎵 Spotify Entegrasyonu
- **PKCE ile OAuth 2.0** güvenli kimlik doğrulaması (AppAuth kütüphanesi)
- Ruh haline ve mesleğe göre AI tabanlı şarkı önerileri
- Kullanıcının Spotify hesabında otomatik çalma listesi oluşturma
- Şarkı arama, liste derleme ve doğrudan Spotify uygulamasında açma

### 🔐 Kullanıcı Oturumu ve Navigasyon
- Animasyonlu geçişlerle 10 ekranlı akış (AnimatedContent)
- Bağlama duyarlı geri navigasyon
- Kullanıcı adı/şifre ile giriş sistemi
- Tam sıfırlama ve yeni gün analizi başlatma

---

## 🏗️ Mimari

```
MVVM (Model-View-ViewModel) — Tek Activity Mimarisi
```

### Temel Bileşenler

| Bileşen | Dosya | Sorumluluk |
|---------|-------|------------|
| **Ana Activity** | `MainActivity.kt` | Uygulama girişi, Spotify auth yönetimi, navigasyon barındırıcı |
| **ViewModel** | `MoodPlannerViewModel.kt` | İş mantığı, durum yönetimi, AI çağrıları, Spotify API |
| **Kamera** | `CameraPreview.kt` | CameraX kurulumu, ön kamera bağlama |
| **Yüz Analizci** | `FaceAnalyzer.kt` | ML Kit yüz algılama, gülümseme tespiti, TTS geri bildirim |
| **Yüz Çizimi** | `FaceOverlay.kt` | Canvas tabanlı yüz kontur/referans noktası çizimi |
| **Spotify Auth** | `SpotifyAuthManager.kt` | OAuth PKCE akış yönetimi |
| **Veri Modelleri** | `User.kt`, `TaskItem.kt`, `ExamDetail.kt`, `WakeMood.kt` | Alan varlıkları |
| **Navigasyon** | `Screen.kt` | Enum tabanlı ekran yönlendirmesi (10 ekran) |
| **Tema** | `ui/theme/*` | Material 3 temalama, dinamik renkler |

---

## 📱 Ekran Akışı

```
Giriş → Meslek Seçimi → [Mesleğe özel sorular] → Ana Analiz
                                                       ↓
                                                 Ruh Hali Onayı
                                                       ↓
                                                 Plan Görüntüleme
                                                       ↓
                                              Spotify Çalma Listesi Süresi
                                                       ↓
                                              Spotify Listesi Oluşturuldu
```

---

## 🛠️ Teknoloji Yığını

| Kategori | Teknoloji |
|----------|-----------|
| **Programlama Dili** | Kotlin |
| **Arayüz Çatısı** | Jetpack Compose (Material 3) |
| **Mimari Desen** | MVVM (StateFlow/MutableState) |
| **Yapay Zeka** | Google Gemini 2.0 Flash (generativeai SDK 0.3.0) |
| **Yüz Algılama** | Google ML Kit (face-detection 16.1.7) |
| **Kamera** | CameraX (camera-core 1.1.0, camera2, lifecycle, view) |
| **Kimlik Doğrulama** | AppAuth (OpenID) 0.11.1 — Spotify PKCE OAuth |
| **Müzik API'si** | Spotify Web API (HttpURLConnection tabanlı) |
| **Konuşma Tanıma** | Android Speech Recognizer (Türkçe yerel ayar) |
| **Metin Sentezi** | Android TextToSpeech (Türkçe yerel ayar) |
| **JSON İşleme** | org.json (yerleşik) + Gson 2.10.1 |
| **Derleme** | Gradle Kotlin DSL, compileSdk 35 |

---

## 🚀 Kurulum

### Ön Gereksinimler
- **Android Studio** Ladybug (2024.2+) veya üzeri
- **Android SDK** 26+ (minimum), 34+ (hedef)
- **JDK 1.8**
- **Google Gemini API Anahtarı** — [Google AI Studio](https://aistudio.google.com/) üzerinden edinin
- **Spotify Geliştirici Uygulaması** — [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) üzerinden oluşturun

### Kurulum Adımları

1. **Projeyi klonlayın:**
   ```bash
   git clone <depo-url>
   cd Proje
   ```

2. **API Anahtarlarını Yapılandırın:**
   
   Proje kök dizinindeki `local.properties` dosyasına ekleyin:
   ```properties
   GEMINI_API_KEY=sizin_gemini_api_anahtariniz
   ```

3. **Spotify'ı Yapılandırın:**
   
   `MainActivity.kt` → `SpotifyAuthHelper` nesnesinde güncelleyin:
   ```kotlin
   const val SPOTIFY_CLIENT_ID = "sizin_spotify_client_id"
   ```
   
   Spotify Developer Dashboard'da yönlendirme URI'sını ekleyin:
   ```
   com.example.mood://spotify-login-callback
   ```

4. **Derleyin ve Çalıştırın:**
   ```bash
   ./gradlew assembleDebug
   ```
   Veya Android Studio'da açıp bir cihaz/emülatör üzerinde çalıştırın.

### Gerekli İzinler
- 📷 **Kamera** — Yüz ifadesi tarama için
- 🎤 **Mikrofon** — Sesli giriş için
- 🌐 **İnternet** — Gemini AI ve Spotify API çağrıları için

---

## 📂 Proje Yapısı

```
Proje/
├── app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml          # Uygulama yapılandırması, izinler
│           ├── java/com/example/mood/
│           │   ├── MainActivity.kt          # Giriş noktası, arayüz ekranları
│           │   ├── MoodPlannerViewModel.kt  # Çekirdek iş mantığı ve durum
│           │   ├── CameraPreview.kt         # CameraX Compose entegrasyonu
│           │   ├── FaceAnalyzer.kt          # ML Kit yüz algılama + TTS
│           │   ├── FaceOverlay.kt           # Canvas yüz kontur çizimi
│           │   ├── FaceGraphicInfo.kt       # Yüz veri modeli
│           │   ├── SpotifyAuthManager.kt    # Spotify OAuth PKCE
│           │   ├── Screen.kt               # Navigasyon ekran enum'u
│           │   ├── WakeMood.kt              # Ruh hali kategorileri
│           │   ├── TaskItem.kt              # Görev veri sınıfı
│           │   ├── ExamDetail.kt            # Sınav veri sınıfı
│           │   ├── User.kt                  # Kullanıcı profil veri sınıfı
│           │   └── ui/theme/               # Material 3 tema dosyaları
│           └── res/                          # Kaynaklar (simgeler, dizeler)
├── build.gradle.kts                          # Kök derleme yapılandırması
├── app/build.gradle.kts                      # Uygulama bağımlılıkları
├── settings.gradle.kts                       # Proje ayarları
└── local.properties                          # API anahtarları (gitignore)
```

---

## 🔮 Gelecek Geliştirmeler

- [ ] Room veritabanı ile kalıcı kullanıcı depolama
- [ ] Spotify token yenileme mekanizması
- [ ] Geçmiş ruh hali izleme ve analitik panosu
- [ ] Plan hatırlatıcıları için bildirim/alarm entegrasyonu
- [ ] Gece modu ruh hali analizi varyantı
- [ ] Ek meslek desteği
- [ ] Çoklu dil desteği
- [ ] Hızlı günlük ruh hali kontrolü için widget

---

## 📄 Lisans

Bu proje eğitim ve kişisel kullanım amacıyla geliştirilmiştir.

---

<p align="center">
  Kotlin, Jetpack Compose, Google Gemini AI ve Spotify API ile 💜 yapılmıştır
</p>
