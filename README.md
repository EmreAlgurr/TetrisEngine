# NES Tetris AI Engine

Orijinal Nintendo NES Tetris (1989) oyununu bir NES emülatörü (FCEUX) üzerinde gerçek zamanlı ve otonom olarak oynayan Java tabanlı bir yapay zeka motoru.

Proje, oyun mekaniklerini taklit eden bir klon değildir; FCEUX emülatörünün RAM adreslerine bağlanıp doğrudan oyun hafızasını okur, en uygun hamleyi ve tuş kombinasyonlarını hesaplar ve sanal kontrolcü üzerinden emülatöre hamleleri basar.

Algoritma tasarımı ve karar heuristiklerinde rekabetçi NES Tetris botu [StackRabbit](https://github.com/GICodeWarrior/StackRabbit)'in stratejilerinden esinlenilmiştir.

---

## Nasıl Çalışıyor?

Sistem iki temel parçanın haberleşmesiyle çalışır:

```
[ NES Tetris (FCEUX) ] 
         │ (RAM Okuma: 0x0400, 0x0042, 0x0019...)
         ▼
[ fceux_bridge.lua ] ──(IPC: bridge_request.txt)──► [ TetrisEngineServer.java ]
         ▲                                                    │ (2-Ply Lookahead Arama)
         │ ◄──(IPC: bridge_response.txt / Token)───────────────┘ (Heuristic Değerlendirme)
         │
[ Tuş Girdileri: A.R.R.D ] ──► Sanal Kontrolcüye Basılır
```

1. **Hafıza Taraması (FCEUX Lua):** `fceux_bridge.lua`, her karede NES hafızasındaki tahta matrisini (`0x0400`), aktif parçayı (`0x0042`), sıradaki parçayı (`0x0019`), level ve satır sayısını okur.
2. **IPC Köprüsü:** FCEUX ortamında ek soket kütüphanelerine veya DLL bağımlılıklarına gerek kalmaması için yerel dosya tabanlı (veya HTTP) bir token senkronizasyonu kullanılır. İstek hazır olduğunda Java tarafına aktarılır.
3. **2-Ply Karar Motoru (`TetrisAI.java`):** Aktif taş için olası tüm rotasyon ve sütun yerleşimlerini dener. Her geçerli yerleşimden sonra "Next" parçasını da simüle ederek 2 hamle sonrasını hesaplar (Lookahead).
4. **Tuş Dizisi Üretimi (`generateInputs`):** Seçilen hedef rotasyon ve sütun, NES D-Pad girdilerine (`A.` rotasyon, `L.`/`R.` yatay hareket, `D` soft drop) çevrilir ve emülatörün kumanda girişine frame bazlı beslenir.

---

## Karar Motoru ve Heuristikler

Motorun tahta puanlama mantığı (`evaluateBoard`) şu parametreler üzerine kuruludur:

- **Tetris Kuyu Stratejisi (Right-Well):** 9. sütun (en sağ sütun) genellikle boş tutularak 'I' parçası geldiğinde 4'lü satır temizliği (Tetris) yapılması hedeflenir. Kuyu kazaen kapanırsa yüksek ceza puanı verilir.
- **Yüzey Düzlüğü (Flatness & Flat Spot):** Sütunlar arasındaki yükseklik farkları üssel olarak cezalandırılır. 'O' ve 'L' parçalarının rahat oturabilmesi için tahtada en az bir düz yüzey (`diff == 0`) aranır.
- **Sol Taraf Desteği (Built-Out Left):** NES Tetris'te DAS (Delayed Auto Shift) gecikmesi nedeniyle taşları sol duvara taşımak sağa göre daha risklidir. Bu nedenle sol sütunların sağ tarafa oranla hafif yüksek tutulması tercih edilir.
- **Delik ve Blokaj Cezaları:** Bir bloğun altında kalan boşluklar (delik) ve bu deliğin üzerindeki dolu hücre sayısı (blokaj) ağır biçimde puan kırar.
- **Panik Modu:** Tahta yüksekliği 10 satırı aştığında kuyu bekleme stratejisi askıya alınır; bot hayatta kalmak için tekli/ikili satırları da temizlemeye odaklanır.

---

## Gereksinimler

- **Java JDK 8 veya üzeri** (Herhangi bir harici kütüphane / bağımlılık gerekmez)
- **FCEUX Emülatörü** (Windows için test edilmiştir)
- **Tetris (USA).nes** ROM dosyası

---

## Kurulum ve Çalıştırma

### 1. Motoru Derleyin ve Başlatın

Proje dizininde terminali açıp motoru derleyin ve çalıştırın:

```bash
javac *.java
java TetrisEngineServer
```

Sunucu açıldığında hem `3000` portundan HTTP dinleyicisi hem de FCEUX için yerel IPC dinleyicisi devreye girer.

### 2. Lua Köprüsünü FCEUX İçinde Çalıştırın

1. FCEUX emülatörünü açın ve **Tetris (USA)** ROM'unu yükleyin.
2. `fceux_bridge.lua` dosyasını bir metin düzenleyiciyle açıp başındaki `BRIDGE_DIR` değişkeninin projenizin bulunduğu dizin yolunu gösterdiğinden emin olun:
   ```lua
   local BRIDGE_DIR = "C:\\Users\\user\\Desktop\\TetrisEngine-main\\"
   ```
3. FCEUX menüsünden:
   **File > Lua > New Lua Script Window** yolunu izleyin.
4. **Browse** butonuyla `fceux_bridge.lua` dosyasını seçip **Run** butonuna basın.
5. Oyunu A-Type modunda başlatın (Level 0, 9 veya 18).

Ekranda yeşil `AI ENGINE: ONLINE` ibaresi belirecek ve bot gelen parçaları oynamaya başlayacaktır. Konsol üzerinde her hamlenin süresi ve detayları listelenir:

```text
⚡ [Lvl 19 | Lines 042] Taş: T  | Next: I  | Karar: Rot=2, Col=5 | Tuşlar: A.R.R.D | 2ms
```

---

## Emülatörsüz Test (Konsol Benchmark)

Emülatörü açmadan sadece karar algoritmasının performansını ve Tetris oranını test etmek isterseniz:

```bash
java TestBotBenchmark
```

Bu komut, rastgele 1000 parçalık bir simülasyon koşturur ve oyun bitiminde yerleştirilen parça sayısı, tekli/ikili/üçlü satırlar, toplam Tetris adedi ve Tetris oranını (TRT) raporlar.

---

## Dosya Yapısı

- `TetrisEngineServer.java` — Emülatör ile köprü kuran sunucu ve dosya tabanlı IPC yöneticisi.
- `TetrisAI.java` — 2-ply arama algoritması, tahta değerlendirme ve tuş girdisi üretici motor.
- `Tetromino.java` — Orijinal NES Tetris blok tanımları, rotasyon durumları ve koordinat ötelemeleri.
- `TestBotBenchmark.java` — Algoritmanın dayanıklılığını ve hızını ölçen simülasyon aracı.
- `fceux_bridge.lua` — FCEUX üzerinde çalışan, RAM adreslerini okuyan ve kontrolcüye basan köprü scripti.
