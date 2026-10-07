# Privacy policy: Chornobyl HamDash

Last updated: 2026-10-07. Applies to the Android app Chornobyl HamDash (`com.hamdash.chornobyl`) by
Yevhen "CUBE" Honchar, in every store and on GitHub.

**Українською нижче.**

## Summary

The app has no accounts, no ads, no analytics and no tracking. It stores its data on your device. It contacts a few
public data services to download reference data; your location is sent only if you choose "My location" as the
weather place, and then rounded to about 10 km.

## Data stored on the device

- Settings (language, theme, time zone, units, data source URLs, weather place) and favorite repeaters.
- Cached reference data: the repeater catalog, space weather values, the last weather forecast, the last radiation readings (grouped into cells of about 2 km) and map tiles.

This data never leaves the device. "Clear cache" in Settings deletes the downloaded data; uninstalling the app deletes
everything.

## Location

- The app asks for location permission only when you tap something that needs it (your position on the map, distances
  to repeaters, sun times for your position). You can refuse, and "Show my location" can be turned off in Settings.
- By default your position stays on the device.
- Only if you pick **My location** as the weather place, your coordinates are rounded to 0.1 degree (about 10 km) and
  sent to Open-Meteo to get the forecast for that area. Any other weather place sends only the fixed coordinates of
  that town.

## Network requests

The app downloads public data from these services. Like any web request, they see your IP address and the request
itself; the app sends no identifiers, accounts or device data.

| Service | What is requested | Their privacy information |
| --- | --- | --- |
| NOAA Space Weather Prediction Center (`services.swpc.noaa.gov`) | Solar flux, K index, X-ray flares | https://www.weather.gov/privacy |
| Open-Meteo (`api.open-meteo.com`) | Weather forecast for the chosen place | https://open-meteo.com/en/terms |
| Safecast (`api.safecast.org`) | Radiation measurements near the Exclusion Zone, requested for the fixed coordinates of the power plant (never your position) | https://safecast.org/ |
| OpenStreetMap tile server (`tile.openstreetmap.org`) or a tile server you set | Map tiles for the area you view | https://osmfoundation.org/wiki/Privacy_Policy |
| A catalog URL you enter in Settings (none by default) | Repeater catalog JSON | The operator of that server |

## Children

The app is a reference tool for licensed radio amateurs and is not directed at children. It collects no personal
data from anyone.

## Changes and contact

Changes to this policy are published in this file in the source repository:
https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/blob/main/PRIVACY.md

Questions: open an issue at https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/issues

---

# Політика конфіденційності: Chornobyl HamDash

Оновлено: 2026-10-07. Стосується Android-застосунку Chornobyl HamDash (`com.hamdash.chornobyl`) від
Yevhen "CUBE" Honchar у всіх магазинах і на GitHub.

## Коротко

У застосунку немає облікових записів, реклами, аналітики й стеження. Дані зберігаються на пристрої. Застосунок
звертається до кількох публічних сервісів, щоб завантажити довідкові дані. Ваше місцезнаходження надсилається лише
тоді, коли ви обираєте "Моє місце" для погоди, і тоді воно округлюється приблизно до 10 км.

## Дані на пристрої

- Налаштування (мова, тема, часовий пояс, одиниці, адреси джерел даних, місце прогнозу) та обрані репітери.
- Кеш довідкових даних: каталог репітерів, космічна погода, останній прогноз погоди й тайли карти.

Ці дані не залишають пристрій. "Очистити кеш" у налаштуваннях видаляє завантажені дані; видалення застосунку видаляє
все.

## Місцезнаходження

- Дозвіл на геолокацію запитується лише тоді, коли ви натискаєте щось, що його потребує (ваше місце на карті, відстані
  до репітерів, схід і захід сонця для вашої позиції). Можна відмовити, а "Показувати моє місцезнаходження" можна
  вимкнути в налаштуваннях.
- За замовчуванням позиція залишається на пристрої.
- Лише якщо ви оберете **Моє місце** для погоди, координати округлюються до 0,1 градуса (близько 10 км) і надсилаються
  до Open-Meteo, щоб отримати прогноз для цього району. Будь-яке інше місце надсилає лише фіксовані координати міста.

## Мережеві запити

Застосунок завантажує публічні дані з сервісів, перелічених у таблиці вище (NOAA SWPC, Open-Meteo, Safecast для радіаційного фону, сервер тайлів
OpenStreetMap або заданий вами, адреса каталогу з налаштувань). Як і будь-який вебзапит, вони бачать вашу IP-адресу та
сам запит; застосунок не надсилає ідентифікаторів, облікових записів чи даних пристрою.

## Діти

Застосунок є довідником для ліцензованих радіоаматорів і не призначений для дітей. Він не збирає персональних даних.

## Зміни та контакти

Зміни публікуються в цьому файлі репозиторію:
https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/blob/main/PRIVACY.md

Питання: https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/issues
