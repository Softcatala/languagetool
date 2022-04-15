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

/**
 *
 */
public class RemotePunctuationRule extends Rule {

  private static final Logger logger = LoggerFactory.getLogger(RemotePunctuationRule.class);

  final String SERVER_URL = "https://api.softcatala.org/punctuation-service/v1/check";
  final int TIMEOUT_MS = 200;

  public RemotePunctuationRule(ResourceBundle messages) throws IOException {
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


  public String connectRemoteServer(String url, String text) {

 /*    System.out.println("connectRemoteServer: '" + text + "'");
     if (text.equals("Això però ningú ho sap")) {
        return "Això, però ningú ho sap";
      }

    return text;
*/
    HttpURLConnection connection = null;

    try {

      text = URLEncoder.encode(text);
      String urlParameters = "text=" + text;
      System.out.println("urlParameters:" + urlParameters);

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
        System.out.println("Line:" + line);
        response.append(line);
        response.append('\n');
      }
      rd.close();
      System.out.println("Response:" + response.toString());
      return response.toString();
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
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {
    final List<RuleMatch> ruleMatches = new ArrayList<>();
    String original = "";

    for (AnalyzedTokenReadings analyzedToken : sentence.getTokens()) {
      original += analyzedToken.getToken();
    }

    String corrected = connectRemoteServer(SERVER_URL, original);

    System.out.println("Original:" + original);
    System.out.println("Corrected:" + corrected);

    //"Aixo ningú ho sap"
    //"Aixo, ningú ho sap"     
    if (original != corrected) {
      System.out.println("Not equal");

      int idxC = 0;
      for (int idxO = 0; idxO < original.length(); idxO++, idxC++) {
        char chO = original.charAt(idxO);
        char chC = corrected.charAt(idxC);

        if (chO == chC) {
          continue;
        }

        System.out.println("chO:" + chO); 
        System.out.println("chC:" + chC);

        if (chC == ',') {

          int start = idxO;
          int length = 2;

          RuleMatch ruleMatch = new RuleMatch(this, sentence, start,
              start + length, "Falta una coma", "Falta una coma");

          String suggestion = String.valueOf(chO) + String.valueOf(chC);
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
}
