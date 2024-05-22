/*
 * Alex Sing
 * Mr. Stutler
 * 5/8/2024
 *
 * Graph.java creates the client used to make calls to the Microsoft Graph API.
 */

package email.sing.tools.dropbox.deduper;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.Request;

public class Graph {
    private static Properties authProperties;
    private static DeviceCodeCredential deviceCodeCredential;
    private static GraphServiceClient<Request> userClient;

    // Client to make API calls.
    public static GraphServiceClient client;


    // Create the parameters for the GraphServiceClient and creates the client.
    public static void initializeGraphForUserAuth(Properties properties, Consumer<DeviceCodeInfo> challenge) throws Exception {
        // Ensure properties isn't null
        if (properties == null) {
            throw new Exception("Properties cannot be null");
        }

        authProperties = properties;

        final String clientId = properties.getProperty("app.clientId");
        final String tenantId = properties.getProperty("app.tenantId");
        final List<String> graphUserScopes = Arrays
                .asList(properties.getProperty("app.graphUserScopes").split(","));

        deviceCodeCredential = new DeviceCodeCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .challengeConsumer(challenge)
                .build();

        final TokenCredentialAuthProvider authProvider =
                new TokenCredentialAuthProvider(graphUserScopes, deviceCodeCredential);

        userClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        setGraphServiceClient(authProvider);
    }

    // Returns the client to be used to make calls to the Microsoft Graph API.
    public static void setGraphServiceClient(IAuthenticationProvider authProvider) {
        client = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    // Returns the user that is signed into Onedrive.
    public static User getUser() throws Exception {
        // Ensure client isn't null
        if (userClient == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        return userClient.me()
                .buildRequest()
                .select("displayName,mail,userPrincipalName")
                .get();
    }
}