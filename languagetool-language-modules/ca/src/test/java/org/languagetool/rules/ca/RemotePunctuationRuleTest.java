package org.languagetool.rules.ca;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Catalan;
import org.languagetool.rules.RuleMatch;
import org.languagetool.*;
import java.util.ResourceBundle;
import org.languagetool.AnalyzedSentence;
import java.util.List;
import java.util.ArrayList;

public class RemotePunctuationRuleTest {

  class RemotePunctuationRuleForTest extends RemotePunctuationRule  {

    public RemotePunctuationRuleForTest(ResourceBundle messages) throws IOException {
        super(messages);
    }

     public String connectRemoteServer(String url, String text) {
        if (text.equals("Això però ningú ho sap")) {
          return "Això, però ningú ho sap";
        }

        if (text.equals("Això però ningú, ho sap")) {
          return"Això però ningú ho sap";
        }

        if (text.equals("Això vol dir una cosa allò una altra")) {
          return "Això vol dir una cosa, allò, una altra";
        }

        return text;
      }
  }

  private RemotePunctuationRuleForTest rule;
  private JLanguageTool lt;

  @Before
  public void setUp() throws IOException {
    rule = new RemotePunctuationRuleForTest(TestTools.getEnglishMessages());
    lt = new JLanguageTool(new Catalan());
  }

  private List<AnalyzedSentence> getAnalyzedSentence(String text) throws IOException {
    List<AnalyzedSentence> sentences = new ArrayList<>();
    AnalyzedSentence sentence = lt.getAnalyzedSentence(text);
    sentences.add(sentence);
    return sentences;
  }

  @Test
  public void testRuleNoCommas() throws IOException {

    assertEquals(0, rule.match(getAnalyzedSentence("Text sense errors")).length);
  }


  @Test
  public void testRuleAddCommas() throws IOException {

    RuleMatch[] matches = rule.match(getAnalyzedSentence("Això però ningú ho sap"));
    assertEquals(1, matches.length);
    assertEquals(4, matches[0].getFromPos());
    assertEquals(9, matches[0].getToPos());
 
    assertEquals(2, rule.match(getAnalyzedSentence("Això vol dir una cosa allò una altra")).length);
  }

  @Test
  public void testRuleRemoveCommas() throws IOException {

    RuleMatch[] matches = rule.match(getAnalyzedSentence("Això però ningú, ho sap"));
    assertEquals(1, matches.length);
    assertEquals(15, matches[0].getFromPos());
    assertEquals(19, matches[0].getToPos());
 
  }

}
