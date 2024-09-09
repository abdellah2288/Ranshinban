package com.ranshinban.ranshinban.Wireless;

import com.ranshinban.ranshinban.utils.errorWindow;
import javafx.beans.property.SimpleStringProperty;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class httpClient
{
    private static CloseableHttpClient mainClient = HttpClientBuilder.create().build();
    private static volatile boolean activated = false;
    private static SimpleStringProperty responseProperty = new SimpleStringProperty();

    static public int requestBeaconList(String url) throws Exception
    {
            if(activated)
            {
                HttpResponse httpResponse = mainClient.execute(new HttpGet("http://" + url), response ->
                {
                    responseProperty.setValue(new String(response.getEntity().getContent().readAllBytes(),"UTF-8"));
                    return response;
                });
                if(httpResponse.getStatusLine().getStatusCode() != 200)
                {
                    errorWindow.raiseErrorWindow("HTTP ERROR: "
                            + httpResponse.getStatusLine().getStatusCode()
                            + "\nGOT RESPONSE: \n"
                            + new String(httpResponse.getEntity().getContent().readAllBytes(),"UTF-8")
                    );
                }
            }
        return 0;
    }

    static public SimpleStringProperty getResponseProperty()
    {
        return responseProperty;
    }
    static public void activateClient()
    {
        activated = true;
    }
    static public void deactivateClient()
    {
        activated = false;
    }
    static public boolean isActivated()
    {
        return activated;
    }
}
