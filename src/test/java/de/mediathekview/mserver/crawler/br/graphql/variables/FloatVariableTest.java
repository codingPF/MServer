/*
 * FloatVariableTest.java
 *
 * Projekt    : MServer
 * erstellt am: 07.12.2017
 * Autor      : Sascha
 *
 * (c) 2017 by Sascha Wiegandt
 */
package de.mediathekview.mserver.crawler.br.graphql.variables;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloatVariableTest {

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testMaxFloatValue() {
      final FloatVariable graphQLVariable = new FloatVariable("degress", Double.MAX_VALUE);
      assertEquals(
              "\"degress\":1.7976931348623157E308", graphQLVariable.getJSONFromVariableOrDefaulNull());
  }

  @Test
  public void testMinFloatValue() {
      final FloatVariable graphQLVariable = new FloatVariable("negativeDegress", Double.MIN_VALUE);
    assertEquals("\"negativeDegress\":4.9E-324", graphQLVariable.getJSONFromVariableOrDefaulNull());
  }

  @Test
  public void testNullFloatValue() {
      final FloatVariable graphQLVariable = new FloatVariable("lenght", null);
    assertEquals("\"lenght\":null", graphQLVariable.getJSONFromVariableOrDefaulNull());
  }
}