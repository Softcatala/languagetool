package org.languagetool.rules.ca;

import org.languagetool.AnalyzedSentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import org.languagetool.rules.*;
import java.net.URLEncoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 */
public class RemotePunctuationRule extends Rule {

  private static final Logger logger = LoggerFactory.getLogger(RemotePunctuationRule.class);

  public RemotePunctuationRule(ResourceBundle messages) throws IOException {
  }

  private HttpURLConnection createConnection(URL url) {
    try {

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setUseCaches(false);
      connection.setDoOutput(true);
      connection.setConnectTimeout(200);
      return connection;
    }
    catch (Exception e) {
      e.printStackTrace();
      logger.error("Could not connect remote service at " + url + " for punctuation service", e);
      return null;
    }
  }


  public String executePost(String url, String text) {
    HttpURLConnection connection = null;

    try {

      text = URLEncoder.encode(text);
      String urlParameters = "text=" + text;

      connection = createConnection(new URL(url));
      if (connection == null)
        return "";

      connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));

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
        response.append('\r');
      }
      rd.close();
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

    // TDB
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
