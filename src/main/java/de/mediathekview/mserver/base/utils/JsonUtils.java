package de.mediathekview.mserver.base.utils;

import java.util.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;

/**
 * A util class to collect useful Json related methods.
 *
 * @author Nicklas Wiegandt (Nicklas2751)<br>
 *         <b>Mail:</b> nicklas@wiegandt.eu<br>
 *         <b>Jabber:</b> nicklas2751@elaon.de<br>
 *         <b>Riot.im:</b> nicklas2751:matrix.elaon.de<br>
 *
 */
public final class JsonUtils {
  private JsonUtils() {
    super();
  }

  /**
   * Checks if the {@link JsonElement} is a {@link JsonObject} and if it has all given elements and
   * if no element is null.
   *
   * @param aJsonElement The element to check.
   * @param aCrawler The crawler which runs this task. When given and a given element is missing the
   *        error counter will be increased and a debug message will be printed.
   * @param aElementIds The elements which it should has.
   * @return true when the element is a {@link JsonObject} and if it has all given elements and if
   *         no element is null.
   */
  public static boolean hasElements(final JsonElement aJsonElement,
      final Optional<? extends AbstractCrawler> aCrawler, final String... aElementIds) {
    return aJsonElement.isJsonObject()
        && hasElements(aJsonElement.getAsJsonObject(), aCrawler, aElementIds);
  }

  /**
   * Checks if the {@link JsonElement} is a {@link JsonObject} and if it has all given elements and
   * if no element is null.
   *
   * @param aJsonElement The element to check.
   * @param aElementIds The elements which it should has.
   * @return true when the element is a {@link JsonObject} and if it has all given elements and if
   *         no element is null.
   */
  public static boolean hasElements(final JsonElement aJsonElement, final String... aElementIds) {
    return hasElements(aJsonElement, Optional.empty(), aElementIds);
  }

  /**
   * Checks if the {@link JsonObject} has all given elements and if no element is null.
   *
   * @param aJsonObject The object to check.
   * @param aCrawler The crawler which runs this task. When given and a given element is missing the
   *        error counter will be increased and a debug message will be printed.
   * @param aElementIds The elements which it should has.
   * @return true when the object has all given elements and if no element is null.
   */
  public static boolean hasElements(final JsonObject aJsonObject,
      final Optional<? extends AbstractCrawler> aCrawler, final String... aElementIds) {
    for (final String elementId : aElementIds) {
      if (!aJsonObject.has(elementId) || aJsonObject.get(elementId).isJsonNull()) {
        if (aCrawler.isPresent()) {
          aCrawler.get().printMissingElementErrorMessage(elementId);
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the {@link JsonObject} has all given elements and if no element is null.
   *
   * @param aJsonObject The object to check.
   *
   * @param aElementIds The elements which it should has.
   * @return true when the object has all given elements and if no element is null.
   */
  public static boolean hasElements(final JsonObject aJsonObject, final String... aElementIds) {
    return hasElements(aJsonObject, Optional.empty(), aElementIds);
  }
  
  /**
   * Gets the value of an attribute
   * @param aJsonObject the object
   * @param aAttributeName the name of the attribute
   * @return the value of the attribute, if it exists, else Optional.empty
   */
  public static Optional<String> getAttributeAsString(final JsonObject aJsonObject, final String aAttributeName) {
    if (aJsonObject.has(aAttributeName)) {
      final JsonElement aElement = aJsonObject.get(aAttributeName);
      if (!aElement.isJsonNull()) {
        return Optional.of(aElement.getAsString());
      }
    }
    
    return Optional.empty();
  }
}