# 📦 endpoint/ Klasörü

Bu klasör, Flight Cache Viewer uygulamasında kullanılan tüm HTTP endpoint'lerinin tanımlandığı modüler sınıfları içerir.

Her bir endpoint, tek bir sorumluluğa sahip olacak şekilde ayrılmıştır. `FlightApiServer` sınıfı yalnızca bu sınıfları çağırarak uygulama başlatımında gerekli rotaları kaydeder.

---

## 📌 Yapı

| Sınıf Adı     | Endpoint URI         | Açıklama                             |
|--------------|-----------------------|--------------------------------------|
| `AddTicket`      | `POST /add-ticket`       | Yeni bir ticket tanımlar            |
| `UpdateTicket`   | `PUT /update-ticket`     | Mevcut ticket'ı günceller           |
| `DeleteTicket`   | `DELETE /delete-ticket`  | Ticket'ı siler                      |
| `GetTickets`     | `GET /tickets`           | Tüm ticket adlarını listeler        |
| `GetMetadata`    | `GET /metadata`          | Tek bir ticket'ın detayını döner    |
| `GetData`        | `GET /data`              | Sayfalı + filtreli veri getirir     |
| `GetCacheInfo`   | `GET /cache-info`        | Yüklenmiş cache TTL bilgisi verir   |

---

## 🎯 Kullanım

Yeni bir endpoint tanımlamak için:
1. Bu klasöre yeni bir sınıf ekle (örnek: `PostSummary.java`).
2. İçinde uygun HTTP metodunu ve route tanımını yap.
3. `FlightApiServer.start()` içinde `PostSummary.register(...)` çağrısını ekle.

---

## 💡 İpuçları

- Her endpoint sınıfı bir `register(...)` fonksiyonu içermeli.
- Gerekli bağımlılıkları parametre olarak al (örnek: `FlightCacheManager`, `QueryConfig`).
- Ortak yapı ve loglama standardı korunmalı.

---

Hazırlayan: **Ada** 🧠

