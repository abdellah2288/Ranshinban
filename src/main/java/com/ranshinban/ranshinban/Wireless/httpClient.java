package com.ranshinban.ranshinban.Wireless;

import com.ranshinban.ranshinban.utils.errorWindow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.nio.charset.StandardCharsets;

public class httpClient
{
    private static final CloseableHttpClient mainClient = HttpClientBuilder.create().build();
    private static volatile BooleanProperty activated = new SimpleBooleanProperty(false);
    private static final SimpleStringProperty responseProperty = new SimpleStringProperty();

    static public int requestBeaconList(String url) throws Exception
    {
            if(activated.getValue())
            {
                HttpResponse httpResponse = mainClient.execute(new HttpGet("http://" + url), response ->
                {
                    responseProperty.setValue(new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8));
                    return response;
                });
                if(httpResponse.getStatusLine().getStatusCode() != 200)
                {
                    errorWindow.raiseErrorWindow("HTTP ERROR: "
                            + httpResponse.getStatusLine().getStatusCode()
                            + "\nGOT RESPONSE: \n"
                            + new String(httpResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8)
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
        activated.setValue(true);
    }
    static public void deactivateClient()
    {
        activated.setValue(false);
    }
    static public BooleanProperty getActivatedProperty()
    {
        return activated;
    }
}
