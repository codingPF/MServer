package de.mediathekview.mserver.crawler.arte.tasks;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import de.mediathekview.mlib.daten.Film;
import de.mediathekview.mserver.crawler.arte.ArteLanguage;
import de.mediathekview.mserver.crawler.arte.json.ArteFilmDeserializer;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractRecrusivConverterTask;

public class ArteFilmConvertTask extends AbstractRecrusivConverterTask<Film, JsonElement> {

  private static final Type OPTIONAL_FILM_TYPE = new TypeToken<Optional<Film>>() {}.getType();
  private static final long serialVersionUID = -7559130997870753602L;
  private final Gson gson;
  private final String authKey;
  private final ArteLanguage language;

  public ArteFilmConvertTask(final AbstractCrawler aCrawler,
      final ConcurrentLinkedQueue<JsonElement> aUrlToCrawlDTOs, final String aAuthKey,
      final ArteLanguage aLanguage) {
    super(aCrawler, aUrlToCrawlDTOs);
    authKey = aAuthKey;
    language = aLanguage;
    gson = new GsonBuilder().registerTypeAdapter(OPTIONAL_FILM_TYPE,
        new ArteFilmDeserializer(crawler, authKey, language)).create();
  }

  @Override
  protected AbstractRecrusivConverterTask<Film, JsonElement> createNewOwnInstance(
      final ConcurrentLinkedQueue<JsonElement> aElementsToProcess) {
    return new ArteFilmConvertTask(crawler, aElementsToProcess, authKey, language);
  }

  @Override
  protected Integer getMaxElementsToProcess() {
    return config.getMaximumUrlsPerTask();
  }

  @Override
  protected void processElement(final JsonElement aElement) {
    final Optional<Film> film = gson.fromJson(aElement, OPTIONAL_FILM_TYPE);
    if (film.isPresent()) {
      taskResults.add(film.get());
      crawler.incrementAndGetActualCount();
      crawler.updateProgress();
    }
  }

}
