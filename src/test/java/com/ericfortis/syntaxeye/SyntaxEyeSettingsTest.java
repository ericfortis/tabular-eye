package com.ericfortis.syntaxeye;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Set;

public class SyntaxEyeSettingsTest extends BasePlatformTestCase {

  private SyntaxEyeSettings settings;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    settings = SyntaxEyeSettings.getInstance();
    settings.setWordsText("");
  }

  public void testEmptyWordList() {
    settings.setWordsText("");
    assertTrue(settings.getWordSet().isEmpty());
    assertTrue(settings.getWordPatterns().isEmpty());
  }

  public void testParsesWordsAndIgnoresBlankLines() {
    settings.setWordsText("NGX_\n\n  ngx_  \n\n");

    assertEquals(Set.of("NGX_", "ngx_"), settings.getWordSet());
  }

  public void testSkipsWordsShorterThanTwoCharacters() {
    settings.setWordsText("a\nNGX_\nb\n");

    assertEquals(Set.of("a", "NGX_", "b"), settings.getWordSet());
    assertEquals(1, settings.getWordPatterns().size());
  }

  public void testPatternsMatchWholeWordsLiterally() {
    settings.setWordsText("NGX_HTTP\n");

    var pattern = settings.getWordPatterns().getFirst();
    assertTrue(pattern.matcher("say NGX_HTTP now").find());
    assertTrue("a quote must not be treated as a metacharacter",
        pattern.matcher("a \"NGX_HTTP\" b").find());
  }

  public void testReusesParsedWordsUntilTheTextChanges() {
    settings.setWordsText("NGX_\nngx_http\n");

    var first = settings.getWordSet();
    var firstPatterns = settings.getWordPatterns();
    assertSame("the parsed set should be cached", first, settings.getWordSet());
    assertSame("the compiled patterns should be cached", firstPatterns, settings.getWordPatterns());

    settings.setWordsText("NGX_HTTP\n");

    var second = settings.getWordSet();
    assertNotSame("changing the text should invalidate the cache", first, second);
    assertEquals(Set.of("NGX_HTTP"), second);
    assertNotSame(firstPatterns, settings.getWordPatterns());
  }

  public void testCachesEvenWhenTheSameTextIsSetAgain() {
    settings.setWordsText("NGX_\n");
    var first = settings.getWordSet();

    settings.setWordsText("NGX_\n");

    assertSame("an unchanged text should keep the cache warm", first, settings.getWordSet());
  }
}