# 🖥️ static/ Klasörü

Bu klasör, Flight Cache Viewer uygulamasının frontend dosyalarını barındırır.

### İçerik:

- `flight.html`: Ana arayüz sayfası
- (İsteğe bağlı) JS, CSS, ikon veya özel görseller

---

## 📄 flight.html Özellikleri

- 🎛️ Ticket seçimi ve yönetimi (ekle, düzenle, sil)
- 📊 Veri listeleme
    - Sayfalama (⏮ ← 1 2 3 → ⏭)
    - Filtreleme input’u
- 📥 CSV çıktısı (sayfa veya tüm veri)
- ⏱️ TTL süresi gösterimi
- Modal dialog formlarıyla temiz ve erişilebilir arayüz

---

## 🌐 Servisleme

SparkJava, bu klasörü şu şekilde servis eder:

```java
staticFiles.location("/static");
```

Bu sayede `flight.html` şu URL'den erişilir:

```
http://localhost:8080/flight.html
```

veya sadece `/` → redirect.

---

## 📌 Genişletme Önerileri

- Favicon ve özel stil dosyaları eklenebilir
- `index.html` tanımlanarak doğrudan giriş yapılabilir
- Frontend framework'e geçiş (Vue/React) ileride düşünülürse bu klasör baz alınır

---

Hazırlayan: **Ada** 🧠

