# 🗃️ metadata.db Yapısı (SQLite)

Bu veritabanı, Flight Cache Viewer uygulamasında tanımlanan tüm ticket sorgularının kalıcı olarak saklandığı yerdir.
Uygulama başlatıldığında bu veritabanı okunur ve `QueryConfig` içeriği oluşturulur.

---

## 📄 Tablo: `queries`

```sql
CREATE TABLE IF NOT EXISTS queries (
    ticket TEXT PRIMARY KEY,
    sql TEXT NOT NULL,
    db_type TEXT,
    host TEXT,
    port TEXT,
    db_name TEXT,
    user TEXT,
    password TEXT,
    cache BOOLEAN,
    ttl_minutes INTEGER
);
```

---

## 🔍 Kolon Açıklamaları

| Alan         | Açıklama                            |
|--------------|-------------------------------------|
| `ticket`     | Ticket adı (benzersiz)              |
| `sql`        | Çalıştırılacak SQL sorgusu          |
| `db_type`    | PostgreSQL, MySQL, Oracle vb.       |
| `host`       | Veritabanı sunucu adresi            |
| `port`       | Port numarası                       |
| `db_name`    | Veritabanı adı                      |
| `user`       | Kullanıcı adı                       |
| `password`   | Şifre                               |
| `cache`      | Otomatik cache yapılsın mı          |
| `ttl_minutes`| Bellekte ne kadar süre tutulacak    |

---

## 🔄 Güncelleme
`ticket` değeri aynıysa `INSERT ... ON CONFLICT DO UPDATE` mekanizması ile kayıt güncellenir.

## ⚠️ Güvenlik Notu
Bu veritabanı kullanıcı adlarını ve şifreleri düz metin olarak tutar. Lokal geliştirme ortamı içindir. Üretim ortamında şifreleme veya vault çözümü önerilir.

---

Hazırlayan: **Ada** 🧠

