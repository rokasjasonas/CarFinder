package lt.carfinder.sites

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lt.carfinder.model.BodyType
import lt.carfinder.model.Car
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Source
import kotlin.math.roundToInt

data class Site(
    val source: Source,
    val home: String,
    val defaultSearch: String,
) {
    val label: String get() = source.name.lowercase().replaceFirstChar { it.uppercase() }

    fun searchPage(page: Int): String = if (page <= 1) defaultSearch else "$defaultSearch?page=$page"
}

object Sites {
    const val BRIDGE = "CarFinder"

    val AUTOPLIUS = Site(Source.AUTOPLIUS, "https://autoplius.lt/", "https://autoplius.lt/skelbimai/naudoti-automobiliai")
    val AUTOGIDAS = Site(Source.AUTOGIDAS, "https://autogidas.lt/", "https://autogidas.lt/skelbimai/automobiliai/")
    val ALL = listOf(AUTOPLIUS, AUTOGIDAS)

    private val listingUrl = Regex("""https?://(?:m\.|www\.)?(?:autoplius|autogidas)\.lt/skelbim(?:ai|as)/[^?#]*?-(\d{5,})\.html""")

    fun listingId(url: String): String? = listingUrl.find(url)?.groupValues?.get(1)

    fun detect(url: String): Site = if (url.contains("autogidas", ignoreCase = true)) AUTOGIDAS else AUTOPLIUS

    fun car(id: String, listings: List<Car>): Car? = listings.firstOrNull { it.id == id }

    /** Injected after every page load. Dismisses the consent wall, then harvests search-result cards or the full listing page. */
    val extractorJs: String = """
(function () {
  if (window.__cfHarvest) return;
  var post = function (payload) {
    var s = JSON.stringify(payload);
    if (window.CarFinder && window.CarFinder.onListing) { window.CarFinder.onListing(s); }
    else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.CarFinder) { window.webkit.messageHandlers.CarFinder.postMessage(s); }
  };
  var num = function (s) { var d = (s || '').replace(/[^\d]/g, ''); return d ? parseInt(d, 10) : null; };
  var listingId = function (u) { var m = (u || '').match(/\/skelbim(?:ai|as)\/[^?#]*?-(\d{5,})\.html/); return m ? m[1] : null; };
  var clean = function (s) { return (s || '').replace(/\s+/g, ' ').trim(); };

  var harvest = function () {
    if (!sessionStorage.getItem('cf-consent')) {
      var clicked = false;
      var consentSel = '.fc-button, #onetrust-accept-btn-handler, button.optanon-allow-all-button, [class*="consent"] button, [class*="cookie"] button, .cmp-button, button';
      document.querySelectorAll(consentSel).forEach(function (b) {
        if (clicked) return;
        var t = (b.textContent || '').replace(/\s+/g, ' ').trim();
        if (t && t.length < 40 && /priimti vis|visiems|sutinku|accept all|allow all|leisti visiems/i.test(t)) {
          var el = b.closest('.fc-button') || b;
          el.click(); clicked = true;
        }
      });
      if (clicked) { sessionStorage.setItem('cf-consent', '1'); setTimeout(function () { location.reload(); }, 500); return; }
    }
    var id = listingId(location.href);
    if (!id) {
      var cards = []; var seen = {};
      var extract = function (cid, card, a) {
        var img = card.querySelector('img');
        var photo = img ? (img.currentSrc || img.src || img.getAttribute('data-src') || (img.getAttribute('srcset') || '').split(' ')[0]) : null;
        if (!photo || photo.indexOf('data:') === 0) return;
        if (/logo|icon|\.svg/i.test(photo)) return;
        var text = clean(card.textContent);
        if (!/\d/.test(text) || text.length > 9000) return;
        var priceSrc = clean((card.querySelector('[class*="price"] strong, [class*="price"], .price') || {}).textContent);
        var price = null; var re = /([\d\u00a0 ]{3,12})\s*€\s*(\/?)/g; var mm;
        var sources = priceSrc && priceSrc !== text ? [priceSrc, text] : [text];
        for (var s = 0; s < sources.length && price === null; s++) {
          while ((mm = re.exec(sources[s])) !== null) { if (mm[2] !== '/') { price = mm[1]; break; } }
        }
        var yearM = text.match(/\b(19|20)\d{2}\b/);
        var kmM = text.match(/([\d\u00a0 ]{2,12})\s*km\b/i);
        var kwM = text.match(/(\d{2,3})\s*kW/i);
        var fuel = (text.match(/benzinas[\/ ]*dujos|benzinas|dyzelinas|elektra|elektrinis|hibridas|plug-in|dujos/i) || [null])[0];
        var gearbox = (text.match(/automatin[ėe]*|mechanin[ėe]*/i) || [null])[0];
        var title = (img && img.getAttribute('alt')) || '';
        if (!title || title.length < 5) {
          var h = card.querySelector('h2, h3, [class*="title"]');
          title = h ? clean(h.textContent) : text.slice(0, 100);
        }
        seen[cid] = 1; window.__cfSeenCards[cid] = 1;
        cards.push({ id: cid, url: a.href.split('?')[0], title: title, price: num(price), year: num(yearM && yearM[0]),
          mileageKm: num(kmM && kmM[1]), fuel: fuel, gearbox: gearbox, engine: kwM ? kwM[0] : null, photos: [photo.split('?')[0]] });
      };
      document.querySelectorAll('article, li').forEach(function (c) {
        var a = c.querySelector('a[href*="/skelbima"]');
        if (!a) return;
        var cid = listingId(a.getAttribute('href'));
        if (!cid || seen[cid] || window.__cfSeenCards[cid]) return;
        if (!(c.querySelector('img') && c.textContent && c.textContent.indexOf('€') >= 0 && c.textContent.length < 9000)) return;
        extract(cid, c, a);
      });
      if (!cards.length) {
        document.querySelectorAll('a[href*="/skelbima"]').forEach(function (a) {
          var cid = listingId(a.getAttribute('href'));
          if (!cid || seen[cid] || window.__cfSeenCards[cid]) return;
          var card = a;
          for (var i = 0; i < 7 && card; i++) {
            if (card.querySelector('img') && card.textContent && card.textContent.indexOf('€') >= 0 && card.textContent.length < 1500) break;
            card = card.parentElement;
          }
          if (!card) return;
          extract(cid, card, a);
        });
      }
      if (cards.length) post({ items: cards });
      return;
    }
    var title = clean(document.querySelector('h1') ? document.querySelector('h1').textContent : '') || document.title.split('|')[0].trim();
    var priceText = '';
    var priceSels = ['.announcement-price .price', '.announcement-price', '[itemprop="price"]', '[class*="price"]:not([class*="old"]):not([class*="other"]):not([class*="calculator"]):not([class*="history"]):not([class*="monthly"]):not([class*="slider"]):not([class*="residual"])'];
    for (var i = 0; i < priceSels.length && !priceText; i++) {
      var els = document.querySelectorAll(priceSels[i]);
      for (var j = 0; j < els.length; j++) {
        var t = clean(els[j].textContent);
        if (/\/\s*m/.test(t)) continue;
        var pm = t.match(/([\d\u00a0 ]{3,12})\s*€/); if (pm) { priceText = pm[1]; break; }
      }
    }
    var params = {};
    document.querySelectorAll('.parameter-row, .parameter, dl > div, dl > div > div, li, tr').forEach(function (row) {
      var l = row.querySelector('.parameter-label, dt, .label, th, [class*="label"]');
      var v = row.querySelector('.parameter-value, dd, .value, td, [class*="value"]');
      if (l && v) { params[l.textContent.trim().toLowerCase()] = clean(v.textContent); }
    });
    var find = function (keys) {
      for (var k in params) { for (var i = 0; i < keys.length; i++) { if (k.indexOf(keys[i]) === 0) return params[k]; } }
      return '';
    };
    var photos = []; var seenP = {};
    var addPhoto = function (u) {
      if (!u) return;
      u = u.split('?')[0];
      if (u.indexOf('data:') === 0) return;
      if (!/\.(jpe?g|webp|png)/i.test(u)) return;
      if (/logo|icon|banner|sprite|placeholder|avatar/i.test(u)) return;
      var key = u.replace(/\/ann_\d+_/, '/ann_').replace(/\/(\d+)x(\d+)\//, '/').replace(/-\d+x\d+\./, '.');
      if (seenP[key]) return; seenP[key] = 1; photos.push(u);
    };
    var src = function (img) { return img.currentSrc || img.src || img.getAttribute('data-src') || img.getAttribute('data-lazy') || (img.getAttribute('srcset') || '').split(' ')[0]; };
    var gallery = document.querySelectorAll('[class*="gallery"] img, [class*="slider"] img, [class*="carousel"] img, [class*="slideshow"] img, [class*="gallery"] picture img');
    if (!gallery.length) gallery = document.querySelectorAll('main img, .body img, article img');
    if (!gallery.length) gallery = document.querySelectorAll('img');
    for (var g = 0; g < gallery.length; g++) { addPhoto(src(gallery[g])); }
    if (!photos.length) { var og = document.querySelector('meta[property="og:image"]'); if (og) addPhoto(og.content); }
    var yearText = find(['pirma registracija', 'pagaminimo data', 'metai', 'year']) || (title.match(/\b(19|20)\d{2}\b/) || [''])[0];
    var payload = {
      id: id, url: location.href.split('?')[0], title: title,
      price: num(priceText),
      year: num((yearText.match(/(19|20)\d{2}/) || [''])[0]),
      mileageKm: num(find(['rida', 'mileage'])),
      fuel: find(['kuro tipas', 'kuras', 'fuel', 'degal']) || null,
      gearbox: find(['pavarų dėžė', 'pavaru deze', 'gearbox', 'transmisij']) || null,
      engine: find(['variklis', 'engine', 'galia']) || null,
      photos: photos.slice(0, 60)
    };
    var key = id + ':' + photos.length + ':' + (payload.price || '');
    if (key !== window.__cfSeenDetail) { window.__cfSeenDetail = key; post(payload); }
  };

  window.__cfHarvest = harvest;
  window.__cfSeenCards = {};
  window.__cfSeenDetail = '';
  harvest();
  setInterval(harvest, 2500);
})();
""".trimIndent()

    @Serializable
    private data class Payload(
        val id: String? = null,
        val url: String = "",
        val title: String = "",
        val price: Int? = null,
        val year: Int? = null,
        val mileageKm: Int? = null,
        val fuel: String? = null,
        val gearbox: String? = null,
        val engine: String? = null,
        val photos: List<String> = emptyList(),
        val items: List<Payload>? = null,
    )

    sealed interface Captured {
        data class One(val car: Car) : Captured
        data class Many(val cars: List<Car>) : Captured
        data object None : Captured
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): Captured {
        val p = runCatching { json.decodeFromString(Payload.serializer(), raw) }.getOrNull() ?: return Captured.None
        p.items?.let { items -> return Captured.Many(items.mapNotNull { toCar(it) }) }
        return toCar(p)?.let { Captured.One(it) } ?: Captured.None
    }

    private fun toCar(p: Payload): Car? {
        val id = p.id ?: listingId(p.url) ?: return null
        if (p.title.isBlank() && p.photos.isEmpty()) return null
        if (junkTitle(p.title)) return null
        val year = p.year?.takeIf { it in 1950..2035 }
        val mileageKm = p.mileageKm?.takeIf { it in 100..1_500_000 }
        if (year == null && mileageKm == null) return null
        val source = detect(p.url).source
        return Car(
            id = id,
            source = source,
            url = p.url,
            title = p.title.ifBlank { "Listing $id" },
            priceEur = p.price?.takeIf { it in 100..2_000_000 },
            year = year,
            mileageKm = mileageKm,
            fuelType = p.fuel?.let { normalizeFuel(it) },
            gearbox = p.gearbox?.let { normalizeGearbox(it) },
            bodyType = inferBodyType(p.title + " " + p.url),
            powerHp = p.engine?.let { parsePowerHp(it) } ?: parsePowerHp(p.title),
            engine = p.engine,
            photos = p.photos,
            capturedAt = nowMillis(),
        )
    }

    private fun nowMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    fun junkTitle(title: String): Boolean {
        val s = title.trim()
        return Regex("supirkim|perkame|išperkame|isperkame|pirktume|^ieškau|^perku|^iestrauk|detali[eu]s?\\b.*(parduod)|parduodu.*dalim", RegexOption.IGNORE_CASE)
            .containsMatchIn(s)
    }

    /** A car worth keeping: not a junk/service ad and looks like a real listing. */
    fun plausible(c: Car): Boolean = !junkTitle(c.title) && (c.year != null || c.mileageKm != null)

    fun normalizeFuel(raw: String): FuelType? {
        val s = raw.lowercase()
        return when {
            "elektr" in s -> FuelType.EV
            "hibrid" in s || "plug-in" in s || "hybrid" in s -> FuelType.HYBRID
            "dyzel" in s || "diesel" in s -> FuelType.DIESEL
            "benzin" in s || "petrol" in s || "dujos" in s || "lpg" in s -> FuelType.PETROL
            else -> null
        }
    }

    fun normalizeGearbox(raw: String): Gearbox? {
        val s = raw.lowercase()
        return when {
            "automatin" in s || "automatic" in s -> Gearbox.AUTOMATIC
            "mechanin" in s || "manual" in s -> Gearbox.MANUAL
            else -> null
        }
    }

    fun inferBodyType(title: String): BodyType? {
        val s = title.lowercase()
        return when {
            Regex("kabriolet|cabrio|convertible|roadster").containsMatchIn(s) -> BodyType.CONVERTIBLE
            Regex("coup[ée]|gt\\b").containsMatchIn(s) -> BodyType.COUPE
            Regex("pick.?up").containsMatchIn(s) -> BodyType.PICKUP
            Regex("vienatūris|vienaturis|monovolume|miniven|kompaktven|\\bvan\\b").containsMatchIn(s) -> BodyType.VAN
            Regex("universal|karavan|touring|combi|kombi|avant|wagon|\\bsw\\b").containsMatchIn(s) -> BodyType.WAGON
            Regex("\\bsuv\\b|visureigis|krosoveris|cross\\b|\\b4x4\\b").containsMatchIn(s) -> BodyType.SUV
            Regex("hecbekas|hectbek|hatchback").containsMatchIn(s) -> BodyType.HATCHBACK
            Regex("sedanas|sedan|limuzinas").containsMatchIn(s) -> BodyType.SEDAN
            else -> null
        }
    }

    fun parsePowerHp(text: String): Int? {
        val ag = Regex("(\\d{2,3})\\s*(?:AG|ag|hp)\\b").find(text)
        if (ag != null) return ag.groupValues[1].toInt()
        val kw = Regex("(\\d{2,3})\\s*kW", RegexOption.IGNORE_CASE).find(text)
        if (kw != null) return (kw.groupValues[1].toInt() * 1.36f).roundToInt()
        return null
    }
}
