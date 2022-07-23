package org.languagetool.rules.ca;

import org.languagetool.AnalyzedSentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import org.languagetool.rules.*;
import java.net.URLEncoder;
import org.languagetool.AnalyzedTokenReadings;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Catalan;
import org.apache.commons.lang3.StringUtils;
import java.nio.charset.Charset;
import org.languagetool.tools.StringTools;
import org.languagetool.UserConfig;


/**
 *
 */
public class RemotePunctuationRule extends TextLevelRule {

  private static final Logger logger = LoggerFactory.getLogger(RemotePunctuationRule.class);
  private UserConfig userConfig;

  String server_url;
  final int TIMEOUT_MS = 2000;
  boolean newOnly = true;

  public RemotePunctuationRule(ResourceBundle messages, UserConfig userConfig) {
    super.setCategory(Categories.PUNCTUATION.getCategory(messages));

    this.userConfig = userConfig;
    server_url = System.getenv("CA_PUNCT_SERVER");

  }

  public void setOnlyNew(boolean _newOnly) {
    newOnly = _newOnly;
  }

  private HttpURLConnection createConnection(URL url, String urlParameters) {
    try {

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setUseCaches(false);
      connection.setDoOutput(true);
      connection.setConnectTimeout(TIMEOUT_MS);
      connection.setReadTimeout(TIMEOUT_MS);
      connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
      return connection;
    }
    catch (Exception e) {
      e.printStackTrace();
      logger.error("Could not connect remote service at " + url + " for punctuation service", e);
      return null;
    }
  }

  public String connectRemoteServer(String url, String inputText) {

    if (StringUtils.isEmpty(url))
      return inputText;

    HttpURLConnection connection = null;

    try {

      String text = URLEncoder.encode(inputText, "utf-8");
      String urlParameters = "text=" + text;

      connection = createConnection(new URL(url), urlParameters);
      if (connection == null)
        return "";

      //Send request
      DataOutputStream wr = new DataOutputStream (
          connection.getOutputStream());
      wr.writeBytes(urlParameters);
      wr.close();

      //Get Response  
      String response = StringTools.streamToString(connection.getInputStream(), "UTF-8");
      ObjectMapper mapper = new ObjectMapper();
      Map map = mapper.readValue(response, Map.class);
      String responseText = (String) map.get("text");
      String responseTime = (String) map.get("time");
//      System.out.println("Response Text:'" + responseText.toString() + "'");
//      System.out.println("Response Time:'" + responseTime.toString() + "'");

      return responseText;
    } catch (Exception e) {
      logger.error("Error while talking to remote service at " + url + " for punctuation service", e);
      return null;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private String getTextFromAnalyzedSentences(List<AnalyzedSentence> sentences) {
    StringBuilder text = new StringBuilder();

    for (AnalyzedSentence sentence : sentences) {
      text.append(getTextFromAnalyzedSentence(sentence));
    }

    return text.toString();
  }

  private String getTextFromAnalyzedSentence(AnalyzedSentence sentence) {
    
    StringBuilder text = new StringBuilder();
    for (AnalyzedTokenReadings analyzedToken : sentence.getTokens()) {
      text.append(analyzedToken.getToken());
    }
    return text.toString();
  }

  private void ShowRuleMatch(RuleMatch ruleMatch) {
    System.out.println("Rule: " + ruleMatch);

  }

  private boolean IsSessionInControlGroup() {
    boolean inControlGroup = true;

    try {

      if (userConfig != null) {
        Long textSessionID;

        textSessionID = userConfig.getTextSessionId();
        inControlGroup = textSessionID % 100 != 0;
        System.out.println("SessionID: " + textSessionID + " in control group: " +  inControlGroup);
      }
    }
    catch (Exception e) {
      logger.error("IsSessionInControlGroup error", e);
      return true;
    }

    return inControlGroup;
  }

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {

    try {
    
        if (IsSessionInControlGroup() == true) {
          return toRuleMatchArray(new ArrayList<>());
        }
        else {
          return doRule(sentences);
        }
    }
    catch (Exception e) {
      logger.error("Error while processing rule", e);
      return toRuleMatchArray(new ArrayList<>());
    }
  }

  //* Select until next word. It can be more than one token (e.g. 'del') */
  private String getUntilEndOfNextWord(AnalyzedTokenReadings[] tokens, int idx) {

    StringBuilder word = new StringBuilder();

    for (;idx < tokens.length; idx++) {
      AnalyzedTokenReadings token = tokens[idx];

      if (!token.isWhitespace())
        break;

      word.append(token.getToken());
    }

    for (;idx < tokens.length; idx++) {
      AnalyzedTokenReadings token = tokens[idx];

      if (token.isWhitespace())
        break;

      word.append(token.getToken());
    }
    return word.toString();
  }

  private RuleMatch[] doRule(List<AnalyzedSentence> sentences) throws IOException {
    final List<RuleMatch> ruleMatches = new ArrayList<>();
    int sentenceOffset = 0;
    JLanguageTool lt = new JLanguageTool(new Catalan());

    String allText = getTextFromAnalyzedSentences(sentences);

    String allCorrected = connectRemoteServer(server_url, allText);

    if (allCorrected == null)
      return toRuleMatchArray(ruleMatches);

    System.out.println("Charset: "  + Charset.defaultCharset());
    System.out.println("Original :'" + allText + "'");
    System.out.println("Corrected:'" + allCorrected + "'");

    List<AnalyzedSentence> correctedSentences = lt.analyzeText(allCorrected);

    if (correctedSentences.size() != sentences.size()) {
      System.out.println("Sentences lists with diferent length:" + correctedSentences.size() + " - " + sentences.size());
      return toRuleMatchArray(ruleMatches);
    }

    System.out.println("Sentences size: " + sentences.size());
    for (int idx = 0; idx < sentences.size(); idx++) {

      AnalyzedSentence originalSentence = sentences.get(idx);
      AnalyzedSentence correctedSentence = correctedSentences.get(idx);
      String originalSentenceText = getTextFromAnalyzedSentence(originalSentence);
      String correctedSentenceText = getTextFromAnalyzedSentence(correctedSentence);

      System.out.println("Original  sentence:'" + originalSentenceText + "'");
      System.out.println("Corrected sentence:'" + correctedSentenceText + "'");

      if (originalSentenceText.equals(correctedSentenceText) == false) {
        System.out.println("Not equal");

        AnalyzedTokenReadings[] originalTokens = originalSentence.getTokens();
        AnalyzedTokenReadings[] correctedTokens = correctedSentence.getTokens();

        int pass = 0;
        for (int idxO = 0, idxC = 0; idxO < originalTokens.length && idxC < correctedTokens.length; idxO++, idxC++, pass++) {
          AnalyzedTokenReadings originalToken = originalTokens[idxO];
          AnalyzedTokenReadings correctedToken = correctedTokens[idxC];

          String originalTokenText = originalTokens[idxO].getToken();
          String correctedTokenText = correctedTokens[idxC].getToken();

          //System.out.println("Original  token: '" + originalTokenText + "' - start: " + originalToken.getStartPos() + " - pass: " + pass);
          //System.out.println("Corrected token: '" + correctedTokenText + "' - start: " + correctedToken.getStartPos()+ " - pass: " + pass);

          if (originalTokenText.equals(correctedTokenText))
            continue;

          if (correctedTokenText.equals(",")) {

            System.out.println("Added comma");
            String nextToken = getUntilEndOfNextWord(originalTokens, idxO + 1);
            int start = sentenceOffset + originalToken.getStartPos();
            int length = nextToken.length() + 1;

            RuleMatch ruleMatch = new RuleMatch(this, originalSentence, start,
                start + length, "Probablement hi falta una coma", "Probablement hi falta una coma");

            String suggestion = correctedTokenText + originalTokenText + nextToken;
            System.out.println("Suggestion:'" + suggestion + "'");
            ruleMatch.addSuggestedReplacement(suggestion);
            ShowRuleMatch(ruleMatch);
            ruleMatches.add(ruleMatch);
            idxC++;
            continue;
          }
          else if (originalTokenText.equals(",")) {
            System.out.println("Removed");
            if (!newOnly) {
              int COMMA_LENGTH = (",").length();

              String nextWord = getUntilEndOfNextWord(originalTokens, idxO + COMMA_LENGTH);

              int start = sentenceOffset + originalToken.getStartPos();
              int length = nextWord.length() + COMMA_LENGTH;

              RuleMatch ruleMatch = new RuleMatch(this, originalSentence, start,
                  start + length, "Sobra la coma", "Sobra la coma");

              String suggestion = correctedTokenText + nextWord;
              System.out.println("Suggestion:'" + suggestion + "'");
              ruleMatch.addSuggestedReplacement(suggestion);
              ShowRuleMatch(ruleMatch);
              ruleMatches.add(ruleMatch);
            }
            idxO++;
            continue;
          }

         /* Target may contain less spaces than source*/
         if (originalToken.isWhitespace() && !correctedToken.isWhitespace()) {
            System.out.println("Space out sync");
            idxC--;
            continue;
          }
        } //for
      } //if (corrected != null && original.equals(corrected) == false) {
      sentenceOffset += originalSentenceText.length();
    }//for (AnalyzedSentence sentence : sentences) {
    return toRuleMatchArray(ruleMatches);
  }

  @Override
  public final String getId() {
    return "CA_REMOTE_PUNCTUATION_RULE";
  }

 @Override
  public String getDescription() {
    return "Detecta errors de puntuació usant un servei remot";
  }

 @Override
  public int minToCheckParagraph() {
    return -1;
  }
}
