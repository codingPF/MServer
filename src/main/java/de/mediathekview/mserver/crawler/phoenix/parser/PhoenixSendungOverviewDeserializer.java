package de.mediathekview.mserver.crawler.phoenix.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.mediathekview.mserver.base.utils.JsonUtils;
import de.mediathekview.mserver.crawler.basic.SendungOverviewDto;
import de.mediathekview.mserver.crawler.phoenix.PhoenixConstants;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PhoenixSendungOverviewDeserializer implements JsonDeserializer<Optional<SendungOverviewDto>> {

  private static final String ELEMENT_CONTENT = "content";
  private static final String ELEMENT_ITEMS = "items";

  private static final String ATTRIBUTE_LINK = "link";
  private static final String ATTRIBUTE_NEXT_URL = "next_url";

  @Override
  public Optional<SendungOverviewDto> deserialize(JsonElement aJsonElement, Type aType, JsonDeserializationContext aContext) {
    final JsonObject jsonObject = aJsonElement.getAsJsonObject();

    if (!jsonObject.has(ELEMENT_CONTENT)) {
      return Optional.empty();
    }

    final JsonObject contentObject = jsonObject.get(ELEMENT_CONTENT).getAsJsonObject();

    final Set<String> itemIds = parseItems(contentObject);
    final Optional<String> nextUrl = JsonUtils.getAttributeAsString(contentObject, ATTRIBUTE_NEXT_URL);

    SendungOverviewDto dto = createDto(itemIds, nextUrl);
    return Optional.of(dto);
  }

  private SendungOverviewDto createDto(final Set<String> itemIds, final Optional<String> nextUrl) {
    SendungOverviewDto dto = new SendungOverviewDto();
    dto.setNextPageId(nextUrl);

    for (String itemId : itemIds) {
      dto.addUrl(PhoenixConstants.URL_FILM_DETAIL_JSON + itemId);
    }

    return dto;
  }

  private Set<String> parseItems(final JsonObject aContentObject) {
    final Set<String> items = new HashSet<>();

    if (aContentObject.has(ELEMENT_ITEMS)) {
      final JsonArray itemArray = aContentObject.get(ELEMENT_ITEMS).getAsJsonArray();
      for (final JsonElement itemElement : itemArray) {

        final Optional<String> htmlUrl = JsonUtils.getAttributeAsString(itemElement.getAsJsonObject(), ATTRIBUTE_LINK);
        if (htmlUrl.isPresent()) {
          items.add(extractIdFromHtmlUrl(htmlUrl.get()));
        }
      }
    }

    return items;
  }

  private static String extractIdFromHtmlUrl(String aHtmlUrl) {
    int indexBegin = aHtmlUrl.lastIndexOf('-') + 1;
    int indexEnd = aHtmlUrl.lastIndexOf('.');

    return aHtmlUrl.substring(indexBegin, indexEnd);
  }
}
