package de.mediathekview.mserver.crawler.srf.tasks;

import com.google.gson.reflect.TypeToken;
import de.mediathekview.mserver.crawler.ard.tasks.ArdTaskBase;
import de.mediathekview.mserver.crawler.basic.*;
import de.mediathekview.mserver.crawler.srf.SrfConstants;
import de.mediathekview.mserver.crawler.srf.parser.SrfTopicDeserializer;

import javax.ws.rs.client.WebTarget;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SrfTopicOverviewTask extends ArdTaskBase<CrawlerUrlDTO, TopicUrlDTO> {

  private static final Type PAGED_ELEMENT_LIST_URL_TYPE_TOKEN =
      new TypeToken<PagedElementListDTO<CrawlerUrlDTO>>() {}.getType();
  private final String baseUrl;
  private final int pageNumber;

  public SrfTopicOverviewTask(
          final AbstractCrawler aCrawler, final ConcurrentLinkedQueue<TopicUrlDTO> aURLsToCrawl, final String baseUrl) {
    this(aCrawler, aURLsToCrawl, baseUrl, 1);
  }

  public SrfTopicOverviewTask(
          final AbstractCrawler aCrawler,
          final ConcurrentLinkedQueue<TopicUrlDTO> aURLsToCrawl,
          final String baseUrl,
          final int aPageNumber) {
    super(aCrawler, aURLsToCrawl);
    this.baseUrl = baseUrl;

    pageNumber = aPageNumber;
    registerJsonDeserializer(PAGED_ELEMENT_LIST_URL_TYPE_TOKEN, new SrfTopicDeserializer());
  }

  @Override
  protected AbstractRecrusivConverterTask<CrawlerUrlDTO, TopicUrlDTO> createNewOwnInstance(
          final ConcurrentLinkedQueue aElementsToProcess) {
    return createNewOwnInstance(aElementsToProcess, 1);
  }

  private AbstractRecrusivConverterTask<CrawlerUrlDTO, TopicUrlDTO> createNewOwnInstance(
          final ConcurrentLinkedQueue aElementsToProcess, final int pageNumber) {
    return new SrfTopicOverviewTask(crawler, aElementsToProcess, baseUrl, pageNumber);
  }

  @Override
  protected void processRestTarget(final TopicUrlDTO aDTO, final WebTarget aTarget) {
    final PagedElementListDTO<CrawlerUrlDTO> results =
        deserialize(aTarget, PAGED_ELEMENT_LIST_URL_TYPE_TOKEN,aDTO);
    taskResults.addAll(results.getElements());

    final Optional<String> nextPageId = results.getNextPage();
    if (nextPageId.isPresent() && pageNumber < crawler.getCrawlerConfig().getMaximumSubpages()) {
      processNextPage(aDTO.getTopic(), nextPageId.get());
    }
  }

  private void processNextPage(final String aTopic, final String aNextPageId) {
    final ConcurrentLinkedQueue<CrawlerUrlDTO> urlDtos = new ConcurrentLinkedQueue<>();
    urlDtos.add(
        new TopicUrlDTO(
            aTopic,
            String.format(SrfConstants.SHOW_OVERVIEW_NEXT_PAGE_URL, baseUrl, aTopic, aNextPageId)));
    final Set<CrawlerUrlDTO> results = createNewOwnInstance(urlDtos, pageNumber + 1).invoke();
    taskResults.addAll(results);
  }
}
