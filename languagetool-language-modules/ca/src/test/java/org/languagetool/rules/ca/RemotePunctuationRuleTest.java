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
         if (text == "Aixo ningú ho sap") {
            return "Aixo, ningú ho sap";
          }

        return "";
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
    assertEquals(1, rule.match(lt.getAnalyzedSentence("Aixo ningú ho sap")).length);
  }
}
