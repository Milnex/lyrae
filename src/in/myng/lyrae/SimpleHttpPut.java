package in.myng.lyrae;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class SimpleHttpPut { 
  public String test() {
    HttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost("http://ec2-54-204-122-234.compute-1.amazonaws.com:3000/user");
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
      nameValuePairs.add(new BasicNameValuePair("id",
          "123456789"));
      nameValuePairs.add(new BasicNameValuePair("name",
              "1dasds9"));
      post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
 
      HttpResponse response = client.execute(post);
      BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      String line = "";
      String result= "";
      while ((line = rd.readLine()) != null) {
    	  result += line;
    	  System.out.println(line);
      }
      return result;

    } catch (IOException e) {
      e.printStackTrace();
    }
	return null;
  }
} 
