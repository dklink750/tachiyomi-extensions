package eu.kanade.tachiyomi.extension.en.mangalife

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Mangalife : ParsedHttpSource() {

    override val name = "Mangalife"

    override val baseUrl = "https://mangalife.us"

    override val lang = "en"

    override val supportsLatest = true

    private val datePattern = Pattern.compile("(\\d+) days? ago")

    override fun popularMangaSelector() = "div#content > p.seriesList chapOnly"

    override fun latestUpdatesSelector() = "div.col-sm-12 col-md-8 leftColumn > div.latestGroup > a.latestSeries"

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/directory", headers)

    override fun latestUpdatesRequest(page: Int) = GET(baseUrl, headers)

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("p.seriesList chapOnly > a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a.latestSeries").first().let {
            val name = it.attr("href").substringAfter("read-online/").substringBefore("-chapter")
            manga.setUrlWithoutDomain("https://mangalife.us/manga/$name")
            manga.title = it.attr("title").substringAfter("Read ").substringBefore(" Chapter")
        }
        return manga
    }

    override fun popularMangaNextPageSelector() = null

    override fun latestUpdatesNextPageSelector() = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = GET("$baseUrl/directory", headers)

    override fun searchMangaSelector() = popularMangaSelector()

    private fun searchMangaSelector(query: String) = "div#content > p.seriesList chapOnly:contains($query)"

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = null

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.author = "Unknown"
        manga.artist = "Unknown"
        manga.genre = "Unknown"
        manga.description = "Unknown"
        manga.status = SManga.UNKNOWN
        manga.thumbnail_url = document.select("div.col-lg-3 col-md-3 col-sm-3 hidden-xs leftImage").first()?.attr("src")
        return manga
    }

    override fun chapterListSelector() = "div.list chapter-list > a.list-group-item"

    override fun chapterFromElement(element: Element): SChapter {
    //    val dateEl = element.select("time.SeriesTime pull-right")

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(element.attr("href"))
        chapter.name = element.select("span.chapterLabel").text()
      //  chapter.date_upload = dateEl.text()?.let { dateParse(it) } ?: 0
        return chapter
    }

    private fun dateParse(dateAsString: String): Long {
        var date: Date
        try {
            date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).parse(dateAsString.replace(Regex("(st|nd|rd|th)"), ""))
        } catch (e: ParseException) {
            val m = datePattern.matcher(dateAsString)

            if (dateAsString != "Today" && m.matches()) {
                val amount = m.group(1).toInt()

                date = Calendar.getInstance().apply {
                    add(Calendar.DATE, -amount)
                }.time
            } else if (dateAsString == "Today"){
                date = Calendar.getInstance().time
            } else return 0
        }

        return date.time
    }

    override fun pageListParse(document: Document): List<Page> {
        val num = document.select("select.PageSelect").last().text().substringAfter("Page ").toInt()
        val url = document.baseUri().substringBeforeLast("1")
        val pages = mutableListOf<Page>()

        for (i in 1..num)
            pages.add(Page(i-1, url + i ))

        pages.getOrNull(0)?.imageUrl = imageUrlParse(document)
        return pages
    }

    override fun imageUrlParse(document: Document) = document.select("img.CurImage").attr("src")

    override fun getFilterList() = super.getFilterList()
}
