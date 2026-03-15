<h1 align="center">Moodİ - Yapay Zeka Tabanlı Akıllı Yaşam ve Duygu Planlayıcısı</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-100%25-purple?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-UI-blue?style=flat-square&logo=android" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/ML_Kit-Face_Detection-green?style=flat-square&logo=google" alt="ML Kit">
  <img src="https://img.shields.io/badge/Gemini_AI-LLM-orange?style=flat-square&logo=google" alt="Gemini AI">
  <img src="https://img.shields.io/badge/Spotify_API-Music-1DB954?style=flat-square&logo=spotify" alt="Spotify">
</p>

## 📌 Özet (Abstract)

**Moodİ**, Android platformu için geliştirilmiş, kullanıcının anlık duygusal durumunu biyometrik ve ses otonomu verileriyle analiz eden ve bu verilere dayanarak hiper-kişiselleştirilmiş günlük planlar üreten gelişmiş bir yaşam asistanıdır. Uygulama, modern makine öğrenimi (ML) modellerini ve büyük dil modellerini (LLM) uç cihazda (edge-device) ve bulut mimarisinde hibrit olarak entegre ederek çalışır. 

Klasik "to-do" (yapılacaklar) uygulamalarından farklı olarak Moodİ; kullanıcının mesleğini, psikolojik durumunu ve hedeflerini dinamik bir vektör uzayında işler.

---

## 🏛️ Sistem Mimarisi ve Algoritma Akışı

Aşağıdaki şema, uygulamanın kamera ve mikrofondan aldığı işlenmemiş (raw) verileri nasıl anlamlı bir yaşam planına ve müzik listesine çevirdiğini (Pipeline) göstermektedir:

```mermaid
graph TD
    classDef user fill:#6200EA,stroke:#fff,stroke-width:2px,color:#fff;
    classDef ml fill:#00C853,stroke:#fff,stroke-width:2px,color:#fff;
    classDef llm fill:#FF6D00,stroke:#fff,stroke-width:2px,color:#fff;
    classDef api fill:#1DB954,stroke:#fff,stroke-width:2px,color:#fff;
    classDef db fill:#2962FF,stroke:#fff,stroke-width:2px,color:#fff;
    classDef ui fill:#D50000,stroke:#fff,stroke-width:2px,color:#fff;

    A((Kullanıcı)):::user -->|Kamera Video Akışı| B[ML Kit Yüz Tanıma 👁️]:::ml
    A -->|Mikrofon Ses Verisi (PCM)| C[Audio Sinyal İşleme 🎙️]:::ml
    
    B -->|Gülümseme & Göz Açıklık Oranı| D{Duygu Vektör Motoru ⚙️}
    C -->|RMS Enerji & Sıfır Geçiş Oranı| D
    
    D -->|Valans (Valence) & Uyarılma (Arousal)| E[Gemini 1.5 Flash AI 🧠]:::llm
    
    E -->|Bağlamsal Çıkarım İstemleri| F[Kişiselleştirilmiş Günlük Plan 📋]
    E -->|Duyguya Uygun Şarkı Önerileri| G[Spotify Web API 🎵]:::api
    
    F --> H[(Room SQLite Veritabanı)]:::db
    G -.->|OAuth 2.0 PKCE Kriptolama| I[Private Çalma Listesi]:::api
    
    H --> J{{Jetpack Compose Arayüzü 📱}}:::ui
    I --> J
```

---

## 🔬 Çekirdek Teknolojiler ve Algoritmik Altyapı

Mimari sistem, kullanıcının "Ruh Hali (Mood)" tespiti ve "Plan Jenerasyonu (Plan Generation)" olmak üzere iki ana analitik motordan oluşmaktadır.

### 1. Görüntü İşleme ve Yüz Tanıma (ML Kit Face Detection Algoritması)
Kullanıcının kameradan alınan anlık görüntüsü, **Google ML Kit Face Detection** API'si kullanılarak işlenir. 
* **Algoritma Mantığı:** Görüntü, sinir ağları (CNN) tabanlı bir Cascade sınıflandırıcıdan geçirilir. Model, yüzdeki 43 farklı nirengi noktasını (landmark) tespit eder.
* **Duygu Vektörizasyonu:** Göz açıklık oranları (`leftEyeOpenProbability`, `rightEyeOpenProbability`) kullanıcının yorgunluk/uyku halini belirlerken, gülümseme olasılığı (`smilingProbability`) doğrudan *Valans (Valence)* değerini hesaplamada kullanılır. Bu iki parametre matrisi, kullanıcının "Enerjik", "Sakin", "Hüzünlü" veya "Stresli" olarak sınıflandırılmasını sağlar.

### 2. Akustik Sinyal İşleme (Ses Tonu Analizi)
Sistem, kullanıcının okuduğu rastgele bir metin sırasındaki ses verisini mikrofon üzerinden (PCM Formatında) saniyede yüzlerce kez örnekler (Sampling) ve sinyal işleme algoritmalarından geçirir.
* **RMS (Root Mean Square) Enerji Hesabı:** Sesi oluşturan sinyalin gücünü bulmak için, zaman alanındaki genliklerin karelerinin ortalamasının karekökü alınır. Yüksek RMS, yüksek *Arousal (Uyarılmışlık)* anlamına gelir.
* **Sıfır Geçiş Oranı (ZCR - Zero-Crossing Rate):** Sinyalin zaman ekseninde sıfır (0) noktasını kesme sıklığını hesaplar. Yüksek frekanslı (tiz ve gergin) sesleri saptamak için proxy bir değer olarak kullanılır ve kullanıcının stres seviyesine yönelik tahminleme (*Heuristic Estimation*) yapar.

### 3. LLM Destekli Dinamik Plan Jenerasyonu (Gemini 1.5 Pro/Flash)
Verilerden elde edilen *Duygu Skoru*, kullanıcının profil detaylarıyla (Örn: Öğrenci ve Sınav Kaygılı) birleştirilerek **GenerativeModel (Gemini AI)** uç noktasına (endpoint) detaylı bir "Prompt Engineering" (İstem Mühendisliği) algoritmasıyla gönderilir.
* **Davranış Modeli:** Sistem, belirli bir sıcaklık (*Temperature = 0.75*) ve çekirdek yapılandırması (*Top-K=1, Top-P=0.95*) kullanarak halüsinasyonları engellerken yaratıcı günlük tavsiyeler üretir.
* **Otonom Alt-Görev Bölümlemesi:** Üretilen metindeki kalın yazılar (Markdown `**bold**`) Compose UI üzerinde ayrıştırılarak kullanıcıya modüler parçalar halinde sunulur (Chunking).

### 4. Spotify API ve PKCE (Prova Edilmiş Güvenlik Algoritması)
Kullanıcının ruh halini ve süresini (Örn: 45 Dakika) hesaplayan uygulama, Spotify Web API'sine entegre olur.
* **OAuth 2.0 PKCE:** `SpotifyAuthManager`, istemci sırrını (Client Secret) mobil cihazda saklamanın yarattığı güvenlik açıklarını önlemek için SHA-256 ile kriptolanmış geçici `code_challenge` token'ları oluşturarak güvenli bir Authorization Code Flow (Oturum Açma Akışı) başlatır.
* **REST Mutasyonu:** HTTP POST istekleriyle kullanıcının kütüphanesine anında "Ruh haline uygun" kapalı (Private) çalma listesi inşa edilir.

### 5. Asenkron İşlemler ve Veri Kalıcılığı (Room ORM)
Veri tabanı işlemleri Android'in **Room SQLite** donanımı üzerinden gerçekleştirilir.
* Mimaride **Coroutines** (Eşyordamlar) ve **Flow** (Veri Akışı) kullanılarak, "Main Thread (Ana İşlem Parçacığı)" bloklanmadan saniyede 60 Kare (60 FPS) akıcı UI deneyimi sağlanır. (ANR sorunları Dispatchers.IO kullanılarak elimine edilmiştir).

---

## 🚀 Kurulum ve Entegrasyon Kılavuzu

### 1. Sistem Gereksinimleri
* Minimum Android Sürümü: API 26 (Android 8.0)
* Önerilen: API 34+
* Çalıştırma Ortamı: Kamerası ve mikrofonu olan gerçek bir Android cihaz önerilir.

### 2. Ortam Değişkenleri (API Anahtarları)
Uygulamanın çalışabilmesi için `.properties` dosyanızda aşağıdaki anahtarları doldurmanız şarttır.

1. **Google Gemini API**
   - [Google AI Studio](https://aistudio.google.com/)'dan API anahtarınızı edinin.
   - Proje kökündeki `local.properties` dosyasına ekleyin: 
     ```properties
     GEMINI_API_KEY=Burdaki_Anahtarinizi_Giriniz
     ```
2. **Spotify Developer Web API**
   - [Spotify Dashboard](https://developer.spotify.com/)'a giderek proje oluşturun.
   - Projedeki `SpotifyAuthManager.kt` dosyasına kendi `SPOTIFY_CLIENT_ID` parametrenizi ekleyin. (Redirect URI'ı `com.example.mood://spotify-login-callback` olarak kaydedin).

### 3. Projeyi İndirme (Clone)
```bash
git clone https://github.com/EmirhanEtem/MoodI.git
cd MoodI
```
Android Studio üzerinden projeyi `Open` diyerek açtıktan sonra Gradle sekronizasyonunu bekleyin ve `Run` butonuna tıklayın.

---

## 💻 Uygulama Katmanları (Clean Architecture Yansıması)
```text
com.example.mood
├── MainActivity.kt         # Entry Point (Navigasyon Grafı ve İzin Yönetimi)
├── MoodPlannerViewModel.kt # State Management, İş Mantığı (LLM, Spotify Call) ve Room Entegrasyonu
├── model/                  # Veritabanı Entity'leri (Kullanıcı, Kayıtlar)
├── screens/                # Jetpack Compose UI Katmanları (AnalysisScreen, ProfileScreen vb.)
├── utils/                  # Makine Öğrenimi (VoiceToneAnalyzer) ve Helper Fonksiyonları
└── theme/                  # Duyguya göre renk paleti değiştiren Material Design Dosyaları
```

## 📜 Lisans Bilgisi
Bu proje Eğitim ve Akademik testler amacıyla oluşturulmuş açık kaynak bir repodur. 

*Geliştirici:* **Emirhan Etem**
