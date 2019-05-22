package de.mediathekview.mserver.crawler.dw.tasks;

import static de.mediathekview.mserver.base.Consts.ATTRIBUTE_VALUE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.mediathekview.mlib.daten.Film;
import de.mediathekview.mlib.daten.FilmUrl;
import de.mediathekview.mlib.daten.Resolution;
import de.mediathekview.mserver.base.utils.HtmlDocumentUtils;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractUrlTask;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import de.mediathekview.mserver.crawler.dw.parser.DWDownloadUrlsParser;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import mServer.crawler.CrawlerTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

public class DWFilmDetailsTask extends AbstractDocumentTask<Film, CrawlerUrlDTO> {

  private static final String URL_SPLITTERATOR = "/";
  private static final long serialVersionUID = 7992707335502505844L;
  private static final Logger LOG = LogManager.getLogger(DWFilmDetailsTask.class);
  private static final String ELEMENT_THEMA = ".artikel";
  private static final String ELEMENT_TITEL = ".group h1";
  private static final String ELEMENT_DATUM = ".group li:eq(0)";
  private static final String ELEMENT_DAUER = ".group li:eq(1)";
  private static final String ELEMENT_DESCRIPTION = ".intro";
  private static final String ELEMENT_FILENAME = ".mediaItem input[name=file_name]";
  private static final String DOWNLOAD_DETAILS_URL_TEMPLATE =
      "%s/playersources/v-%s";

  private static final String DATE_REGEX_PATTERN = "(?<=Datum\\s)[\\.\\d]+";

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN);
  private String baseUrl;

  public DWFilmDetailsTask(final AbstractCrawler aCrawler,
      final ConcurrentLinkedQueue<CrawlerUrlDTO> aUrlToCrawlDTOs, final String aBaseUrl) {
    super(aCrawler, aUrlToCrawlDTOs);
    baseUrl = aBaseUrl;
  }

  private void addDownloadUrls(final CrawlerUrlDTO aUrlDTO, Optional<String> fileName, final Film film) {
    final String pageId =
        aUrlDTO.getUrl().substring(aUrlDTO.getUrl().lastIndexOf(URL_SPLITTERATOR) + 1);
    final String videoId = pageId.substring(pageId.indexOf('-') + 1);
    final String downloadUrl = String.format(DOWNLOAD_DETAILS_URL_TEMPLATE, baseUrl, videoId);

    try {
      if (fileName.isPresent()) {
        film.addUrl(Resolution.SMALL, CrawlerTool.uriToFilmUrl(new URL(fileName.get())));
      }

      final WebTarget target = ClientBuilder.newClient().target(new URL(downloadUrl).toString());
      Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
      if (response.getStatus() == 200) {

        final Type urlMapType = new TypeToken<Map<Resolution, FilmUrl>>() {
        }.getType();
        final Gson gson =
            new GsonBuilder().registerTypeAdapter(urlMapType, new DWDownloadUrlsParser()).create();

        film.addAllUrls(gson.fromJson(response.readEntity(String.class), urlMapType));
      } else {
        LOG.error("DWFilmDetailsTask: Error reading url " + downloadUrl + ": " + response.getStatus());
      }

    } catch (final MalformedURLException malformedURLException) {
      LOG.error(
          String.format("Something wen't wrong on building a download url \"%s\".", downloadUrl));
    }
  }

  private Optional<LocalDate> parseDate(final Optional<String> aDateText) {
    if (aDateText.isPresent()) {
      final Matcher dateMatcher = Pattern.compile(DATE_REGEX_PATTERN).matcher(aDateText.get());
      if (dateMatcher.find()) {
        return Optional.of(LocalDate.parse(dateMatcher.group(), DATE_FORMATTER));
      }
    }
    return Optional.empty();
  }

  @Override
  protected AbstractUrlTask<Film, CrawlerUrlDTO> createNewOwnInstance(
      final ConcurrentLinkedQueue<CrawlerUrlDTO> aURLsToCrawl) {
    return new DWFilmDetailsTask(crawler, aURLsToCrawl, baseUrl);
  }

  @Override
  protected void processDocument(final CrawlerUrlDTO aUrlDTO, final Document aDocument) {
    final Optional<String> titel = HtmlDocumentUtils.getElementString(ELEMENT_TITEL, aDocument);
    final Optional<String> thema = HtmlDocumentUtils.getElementString(ELEMENT_THEMA, aDocument);
    final Optional<String> dateText = HtmlDocumentUtils.getElementString(ELEMENT_DATUM, aDocument);
    final Optional<String> description = HtmlDocumentUtils.getElementString(ELEMENT_DESCRIPTION, aDocument);
    final Optional<String> fileName = HtmlDocumentUtils.getElementAttributeString(ELEMENT_FILENAME, ATTRIBUTE_VALUE, aDocument);
    final Optional<Duration> dauer = parseDuration(aDocument);

    if (checkPreConditions(titel, thema, dauer)) {
      final Optional<LocalDate> time = parseDate(dateText);

      createFilm(aUrlDTO, titel.get(), thema.get(), description, fileName, dauer.get(), time);
    }
  }

  private void createFilm(CrawlerUrlDTO aUrlDTO, String title, String topic, Optional<String> description,
      Optional<String> fileName, Duration duration, Optional<LocalDate> time) {
    try {
      final Film newFilm = new Film(UUID.randomUUID(), crawler.getSender(), title,
          topic, time.orElse(LocalDate.now()).atStartOfDay(), duration);
      description.ifPresent(newFilm::setBeschreibung);
      newFilm.setWebsite(new URL(aUrlDTO.getUrl()));
      addDownloadUrls(aUrlDTO, fileName, newFilm);

      final Optional<FilmUrl> defaultUrl = newFilm.getDefaultUrl();
      if (defaultUrl.isPresent()) {
        newFilm.setGeoLocations(CrawlerTool.getGeoLocations(crawler.getSender(),
            defaultUrl.get().getUrl().toString()));
      }

      taskResults.add(newFilm);
      crawler.incrementAndGetActualCount();
      crawler.updateProgress();
    } catch (final MalformedURLException malformedURLException) {
      LOG.fatal("The website URL can't be parsed.", malformedURLException);
      crawler.printInvalidUrlErrorMessage(aUrlDTO.getUrl());
    }
  }

  private boolean checkPreConditions(Optional<String> title, Optional<String> topic, Optional<Duration> duration) {
    if (!topic.isPresent()) {
      crawler.printMissingElementErrorMessage("Thema");
      return false;
    }
    if (!title.isPresent()) {
      crawler.printMissingElementErrorMessage("Titel");
      return false;
    }
    if (!duration.isPresent()) {
      crawler.printMissingElementErrorMessage("Dauer");
      return false;
    }

    return true;
  }

  private Optional<Duration> parseDuration(final Document aDocument) {
    final Optional<String> dauerText = HtmlDocumentUtils.getElementString(ELEMENT_DAUER, aDocument);

    final Optional<Duration> dauer;
    if (dauerText.isPresent()) {
      dauer = HtmlDocumentUtils.parseDuration(dauerText);
    } else {
      dauer = Optional.empty();
    }
    return dauer;
  }


}