package de.mediathekview.mserver.crawler.ard;

import de.mediathekview.mlib.daten.Film;
import de.mediathekview.mlib.daten.Sender;
import de.mediathekview.mlib.messages.listener.MessageListener;
import de.mediathekview.mserver.base.config.MServerConfigManager;
import de.mediathekview.mserver.base.messages.ServerMessages;
import de.mediathekview.mserver.crawler.ard.tasks.ArdDayPageTask;
import de.mediathekview.mserver.crawler.ard.tasks.ArdFilmDetailTask;
import de.mediathekview.mserver.crawler.ard.tasks.ArdTopicPageTask;
import de.mediathekview.mserver.crawler.ard.tasks.ArdTopicsOverviewTask;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import de.mediathekview.mserver.progress.listeners.SenderProgressListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ArdCrawler extends AbstractCrawler {

  private static final Logger LOG = LogManager.getLogger(ArdCrawler.class);

  public ArdCrawler(
      final ForkJoinPool aForkJoinPool,
      final Collection<MessageListener> aMessageListeners,
      final Collection<SenderProgressListener> aProgressListeners,
      final MServerConfigManager rootConfig) {
    super(aForkJoinPool, aMessageListeners, aProgressListeners, rootConfig);
  }

  @Override
  public Sender getSender() {
    return Sender.ARD;
  }

  private ConcurrentLinkedQueue<CrawlerUrlDTO> createDayUrlsToCrawl() {
    final ConcurrentLinkedQueue<CrawlerUrlDTO> dayUrlsToCrawl = new ConcurrentLinkedQueue<>();

    final LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i <= crawlerConfig.getMaximumDaysForSendungVerpasstSection(); i++) {
      final String url =
          new ArdUrlBuilder(ArdConstants.BASE_URL, ArdConstants.DEFAULT_CLIENT)
              .addSearchDate(now.minusDays(i))
              .addSavedQuery(
                  ArdConstants.QUERY_DAY_SEARCH_VERSION, ArdConstants.QUERY_DAY_SEARCH_HASH)
              .build();

      dayUrlsToCrawl.offer(new CrawlerUrlDTO(url));
    }
    return dayUrlsToCrawl;
  }

  @Override
  protected RecursiveTask<Set<Film>> createCrawlerTask() {

    try {
      final ConcurrentLinkedQueue<ArdFilmInfoDto> shows = new ConcurrentLinkedQueue<>();
      shows.addAll(getDaysEntries());
      getTopicsEntries()
          .forEach(
              show -> {
                if (!shows.contains(show)) {
                  shows.add(show);
                }
              });

      printMessage(
          ServerMessages.DEBUG_ALL_SENDUNG_FOLGEN_COUNT, getSender().getName(), shows.size());
      getAndSetMaxCount(shows.size());

      return new ArdFilmDetailTask(this, shows);
    } catch (final InterruptedException | ExecutionException ex) {
      LOG.fatal("Exception in ARD crawler.", ex);
    }
    return null;
  }

  private Set<ArdFilmInfoDto> getDaysEntries() throws InterruptedException, ExecutionException {
    final ArdDayPageTask dayTask = new ArdDayPageTask(this, createDayUrlsToCrawl());
    final Set<ArdFilmInfoDto> shows = forkJoinPool.submit(dayTask).get();

    printMessage(
        ServerMessages.DEBUG_ALL_SENDUNG_FOLGEN_COUNT, getSender().getName(), shows.size());

    return shows;
  }

  private Set<ArdFilmInfoDto> getTopicsEntries() throws ExecutionException, InterruptedException {
    final ArdTopicsOverviewTask topicsTask =
        new ArdTopicsOverviewTask(this, createTopicsOverviewUrl());

    final ConcurrentLinkedQueue<CrawlerUrlDTO> topicUrls =
        new ConcurrentLinkedQueue<>(forkJoinPool.submit(topicsTask).get());

    final ArdTopicPageTask topicTask = new ArdTopicPageTask(this, topicUrls);
    final Set<ArdFilmInfoDto> filmInfos = forkJoinPool.submit(topicTask).get();

    printMessage(
        ServerMessages.DEBUG_ALL_SENDUNG_FOLGEN_COUNT, getSender().getName(), filmInfos.size());

    return filmInfos;
  }

  private ConcurrentLinkedQueue<CrawlerUrlDTO> createTopicsOverviewUrl() {
    final ConcurrentLinkedQueue<CrawlerUrlDTO> urls = new ConcurrentLinkedQueue<>();

    final String url =
        new ArdUrlBuilder(ArdConstants.BASE_URL, ArdConstants.DEFAULT_CLIENT)
            .addSavedQuery(ArdConstants.QUERY_TOPICS_VERSION, ArdConstants.QUERY_TOPICS_HASH)
            .build();

    urls.add(new CrawlerUrlDTO(url));

    return urls;
  }
}