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


public class RemotePunctuationRuleTest {

  class RemotePunctuationRuleForTest extends RemotePunctuationRule  {

    public RemotePunctuationRuleForTest(ResourceBundle messages) throws IOException {
        super(messages);
    }

     public String connectRemoteServer(String url, String text) {
        if (text.equals("Això però ningú ho sap")) {
          return "Això, però ningú ho sap";
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

  @Test
  public void testRule() throws IOException {

    assertEquals(0, rule.match(lt.getAnalyzedSentence("Text sense errors")).length);

    RuleMatch[] matches = rule.match(lt.getAnalyzedSentence("Això però ningú ho sap"));
    assertEquals(1, matches.length);
    assertEquals(4, matches[0].getFromPos());
    assertEquals(6, matches[0].getToPos());
 
    assertEquals(2, rule.match(lt.getAnalyzedSentence("Això vol dir una cosa allò una altra")).length);
  }
}
