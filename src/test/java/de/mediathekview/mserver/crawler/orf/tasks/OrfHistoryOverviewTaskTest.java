package de.mediathekview.mserver.crawler.orf.tasks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.mediathekview.mlib.daten.Sender;
import de.mediathekview.mserver.base.config.MServerConfigManager;
import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.crawler.basic.TopicUrlDTO;
import de.mediathekview.mserver.crawler.orf.OrfConstants;
import de.mediathekview.mserver.crawler.orf.OrfCrawler;
import de.mediathekview.mserver.testhelper.JsoupMock;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class OrfHistoryOverviewTaskTest {

  @Mock
  JsoupConnection jsoupConnection;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private final TopicUrlDTO[] expectedUrls =
      new TopicUrlDTO[]{
          new TopicUrlDTO(
              "Die Geschichte des Burgenlands",
              "https://tvthek.orf.at/history/Die-Geschichte-des-Burgenlands/9236430"),
          new TopicUrlDTO(
              "Die Geschichte Niederösterreichs",
              "https://tvthek.orf.at/history/Die-Geschichte-Niederoesterreichs/8378971"),
          new TopicUrlDTO(
              "Volksgruppen in Österreich",
              "https://tvthek.orf.at/history/Volksgruppen-in-Oesterreich/13557924")
      };

  @Test
  public void test() throws Exception {
    final OrfCrawler crawler = Mockito.mock(OrfCrawler.class);
    when(crawler.getCrawlerConfig())
        .thenReturn(MServerConfigManager.getInstance().getSenderConfig(Sender.ORF));
    Document document = JsoupMock
        .getFileDocument(OrfConstants.URL_ARCHIVE, "/orf/orf_history_overview.html");
    when(jsoupConnection.getDocumentTimeoutAfter(eq(OrfConstants.URL_ARCHIVE), anyInt()))
        .thenReturn(document);

    final OrfHistoryOverviewTask target = new OrfHistoryOverviewTask(crawler, jsoupConnection);

    final ConcurrentLinkedQueue<TopicUrlDTO> actual = target.call();
    assertThat(actual, notNullValue());
    assertThat(actual.size(), equalTo(expectedUrls.length));
    assertThat(actual, Matchers.containsInAnyOrder(expectedUrls));
  }
}