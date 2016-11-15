package tech.waid.app.Services;


import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by felip on 13/11/2016.
 */
public final  class UserService {

   public static void InsertPosition(double x, double y, double z)
   {
       try {
           String url = "http://waidwebapi.azurewebsites.net/api/UserInfo?X=" + Double.toString(x) + "&Y=" + Double.toString(y) + "&Z=" + Double.toString(z);
           URI urlToSend = new URI(url);
           LongOperation tsk = new LongOperation();
           tsk.execute(url);
       }
       catch (URISyntaxException ex) {

       }
       catch (Exception e)
       {

       }
    }



    public static InputStream downloadUrl(final URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(60000 /* milliseconds */);
        conn.setConnectTimeout(60000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }
// Create a new HttpClient and Post Header
       //"http://waidwebapi.azurewebsites.net/api/UserInfo");


    }

