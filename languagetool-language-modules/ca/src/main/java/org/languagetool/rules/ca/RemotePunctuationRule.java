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

/**
 *
 */
public class RemotePunctuationRule extends TextLevelRule {

  private static final Logger logger = LoggerFactory.getLogger(RemotePunctuationRule.class);

  final String SERVER_URL = "https://api.softcatala.org/punctuation-service/v1/check";
  final int TIMEOUT_MS = 200;

  public RemotePunctuationRule(ResourceBundle messages) throws IOException {
    super.setCategory(Categories.PUNCTUATION.getCategory(messages));
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

  private String restoreTrailingSpacesAtStart(String original, String corrected) {
      String responseWithSpaces = "";
      for (char ch: original.toCharArray()) {
        if (Character.isWhitespace(ch) == false)
          break;

        responseWithSpaces += String.valueOf(ch);
      }
      responseWithSpaces += corrected;
      return responseWithSpaces;
  }


  public String connectRemoteServer(String url, String inputText) {

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

      String responseWithSpaces = restoreTrailingSpacesAtStart(inputText, responseText);
      System.out.println("Response Text:'" + responseWithSpaces.toString() + "'");

      return responseWithSpaces;
    } catch (Exception e) {
      logger.error("Error while talking to remote service at " + url + " for punctuation service", e);
      return null;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    final List<RuleMatch> ruleMatches = new ArrayList<>();

    int sentenceOffset = 0;

    for (AnalyzedSentence sentence : sentences) {
      String original = "";

      for (AnalyzedTokenReadings analyzedToken : sentence.getTokens()) {
        original += analyzedToken.getToken();
      }

      System.out.println("Original :'" + original + "'");
      String corrected = connectRemoteServer(SERVER_URL, original);
      System.out.println("Corrected:'" + corrected + "'");

      if (corrected != null && original.equals(corrected) == false) {
        System.out.println("Not equal");

        for (int idxO = 0, idxC = 0; idxO < original.length() && idxC < corrected.length(); idxO++, idxC++) {
          char chO = original.charAt(idxO);
          char chC = corrected.charAt(idxC);

          if (chO == chC) {
            continue;
          }

          System.out.println("chO:" + chO);
          System.out.println("chC:" + chC);

          if (chC == ',') {

            int start = sentenceOffset + idxO;
            int length = 1 + 1;

            RuleMatch ruleMatch = new RuleMatch(this, sentence, start,
                start + length, "Falta una coma", "Falta una coma");

            String suggestion = String.valueOf(chC) + String.valueOf(chO);
            System.out.println("Suggestion:'" + suggestion + "'");
            ruleMatch.addSuggestedReplacement(suggestion);
            ruleMatches.add(ruleMatch);
            idxC++;

          }  else if (chC == ' ') {
            System.out.println("Removed");
            break;
          }
          else {
            System.out.println("Do not know what to do");
            break;
          }
        }      
      }
      sentenceOffset += original.length();
    }
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
