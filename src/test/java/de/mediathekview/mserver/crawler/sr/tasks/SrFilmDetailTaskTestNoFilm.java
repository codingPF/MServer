package de.mediathekview.mserver.crawler.sr.tasks;

import de.mediathekview.mlib.daten.Film;
import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.testhelper.JsoupMock;
import org.jsoup.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class SrFilmDetailTaskTestNoFilm extends SrTaskTestBase {

  private final String requestUrl;
  private final String filmPageFile;
  private final String theme;
  private final String videoDetailsUrl;
  private final String videoDetailsFile;

  @Mock JsoupConnection jsoupConnection;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  public SrFilmDetailTaskTestNoFilm(
      final String aRequestUrl,
      final String aFilmPageFile,
      final String aTheme,
      final String videoDetailsUrl,
      final String videoDetailsFile) {
    requestUrl = aRequestUrl;
    filmPageFile = aFilmPageFile;
    theme = aTheme;
    this.videoDetailsUrl = videoDetailsUrl;
    this.videoDetailsFile = videoDetailsFile;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "https://www.sr-mediathek.de/index.php?seite=7&id=15808&pnr=0",
            "/sr/sr_podcast_page.html",
            "Abendrot",
            null,
            null
          },
          {
            "https://www.sr-mediathek.de/index.php?seite=7&id=57773",
            "/sr/sr_audio_page.html",
            "Bücherlese",
            null,
            null
          },
          {
            "https://www.sr-mediathek.de/index.php?seite=7&id=39741",
            "/sr/sr_film_page3_fsk.html",
            "Tatort",
            "/sr_player/mc.php?id=39741&tbl=&pnr=0&hd=0&devicetype=",
            "/sr/sr_film_video_details3_fsk.json"
          }
        });
  }

  @Test
  public void test() throws IOException {
    final Connection connection = JsoupMock.mock(requestUrl, filmPageFile);
    when(jsoupConnection.getConnection(eq(requestUrl))).thenReturn(connection);

    if (videoDetailsUrl != null) {
      setupSuccessfulJsonResponse(videoDetailsUrl, videoDetailsFile);
    }

    final Set<Film> actual = executeTask(theme, requestUrl);

    assertThat(actual, notNullValue());
    assertThat(actual.size(), equalTo(0));
  }

  private Set<Film> executeTask(final String aTheme, final String aRequestUrl) {
    return new SrFilmDetailTask(
            createCrawler(), createCrawlerUrlDto(aTheme, aRequestUrl), jsoupConnection)
        .invoke();
  }
}
