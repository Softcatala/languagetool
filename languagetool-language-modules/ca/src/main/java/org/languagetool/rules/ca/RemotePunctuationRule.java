package org.languagetool.rules.ca;

import org.languagetool.AnalyzedSentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.languagetool.Language;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;
import org.languagetool.rules.*;
import org.apache.commons.lang3.NotImplementedException;

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


  public static String executePost(String targetURL, String urlParameters) {
    HttpURLConnection connection = null;

    try {
      //Create connection
      URL url = new URL(targetURL);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", 
          "application/x-www-form-urlencoded");

      connection.setRequestProperty("Content-Length",
          Integer.toString(urlParameters.getBytes().length));
      connection.setRequestProperty("Content-Language", "en-US");

      connection.setUseCaches(false);
      connection.setDoOutput(true);

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
      e.printStackTrace();
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
