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
/**
 *
 */
public class RemotePunctuationRule extends TextLevelRule {

  private static final Logger logger = LoggerFactory.getLogger(RemotePunctuationRule.class);

  //final String SERVER_URL = "https://api.softcatala.org/punctuation-service/v1/check";
  String SERVER_URL;
  final int TIMEOUT_MS = 2000;

  public RemotePunctuationRule(ResourceBundle messages) throws IOException {
    super.setCategory(Categories.PUNCTUATION.getCategory(messages));

    SERVER_URL = System.getenv("CA_PUNCT_SERVER");
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

    if (StringUtils.isEmpty(SERVER_URL))
      return inputText;

    HttpURLConnection connection = null;

    try {

      String text = URLEncoder.encode(inputText);
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
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
      String line;
      while ((line = rd.readLine()) != null) {
        response.append(line);
        response.append('\n');
      }
      rd.close();

      ObjectMapper mapper = new ObjectMapper();
      Map map = mapper.readValue(response.toString(), Map.class);
      String responseText = (String) map.get("text");
      Double responseTime = (Double) map.get("time");
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

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    final List<RuleMatch> ruleMatches = new ArrayList<>();
    int sentenceOffset = 0;
    JLanguageTool lt = new JLanguageTool(new Catalan());

    String allText = getTextFromAnalyzedSentences(sentences);

    String allCorrected = connectRemoteServer(SERVER_URL, allText);
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

      if (correctedSentenceText != null && originalSentenceText.equals(correctedSentenceText) == false) {
        System.out.println("Not equal");

        AnalyzedTokenReadings[] originalTokens = originalSentence.getTokens();
        AnalyzedTokenReadings[] correctedTokens = correctedSentence.getTokens();

        for (int idxO = 0, idxC = 0; idxO < originalTokens.length && idxC < correctedTokens.length; idxO++, idxC++) {
          AnalyzedTokenReadings originalToken = originalTokens[idxO];
          AnalyzedTokenReadings correctedToken = correctedTokens[idxC];
          String originalTokenText = originalTokens[idxO].getToken();
          String correctedTokenText = correctedTokens[idxC].getToken();

  //        System.out.println("Original  token: '" + originalTokenText + "' - start: " + originalToken.getStartPos());
  //        System.out.println("Corrected token: '" + correctedTokenText + "' - start: " + correctedToken.getStartPos());

          if (originalTokenText.equals(correctedTokenText))
            continue;

          if (correctedTokenText.equals(",")) {

            System.out.println("Added comma");
            String nextToken = originalTokens[idxO + 1].getToken();
            int start = sentenceOffset + originalToken.getStartPos();
            int length = nextToken.length() + 1;

            RuleMatch ruleMatch = new RuleMatch(this, originalSentence, start,
                start + length, "Falta una coma", "Falta una coma");

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

            String nextToken = originalTokens[idxO + 1].getToken();
            String nextToken2 = originalTokens[idxO + 2].getToken();
            int start = sentenceOffset + originalToken.getStartPos();
            int length = nextToken.length() + nextToken2.length() + 1;

            RuleMatch ruleMatch = new RuleMatch(this, originalSentence, start,
                start + length, "Sobra la coma", "Sobra la coma");

            String suggestion = correctedTokenText + nextToken + nextToken2;
            System.out.println("Suggestion:'" + suggestion + "'");
            ruleMatch.addSuggestedReplacement(suggestion);
            ShowRuleMatch(ruleMatch);
            ruleMatches.add(ruleMatch);
            idxO++;
            continue;
          }
          /*else {
            System.out.println("Do not know what to do");
            break;
          }*/
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
