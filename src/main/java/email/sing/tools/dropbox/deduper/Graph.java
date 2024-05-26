/*
 * Alex Sing
 * Mr. Stutler
 * 5/8/2024
 *
 * Graph.java creates the client used to make calls to the Microsoft Graph API.
 */

package email.sing.tools.dropbox.deduper;

/*
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

    // Returns the user that is signed in to Onedrive.
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
 */

import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.MessageCollectionResponse;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
// </ImportSnippet>

public class Graph {
    // <UserAuthConfigSnippet>
    private static Properties _properties;
    private static DeviceCodeCredential _deviceCodeCredential;
    private static GraphServiceClient _userClient;

    public static void initializeGraphForUserAuth(Properties properties, Consumer<DeviceCodeInfo> challenge) throws Exception {
        // Ensure properties isn't null
        if (properties == null) {
            throw new Exception("Properties cannot be null");
        }

        _properties = properties;

        final String clientId = properties.getProperty("app.clientId");
        final String tenantId = properties.getProperty("app.tenantId");
        final String[] graphUserScopes = properties.getProperty("app.graphUserScopes").split(",");

        _deviceCodeCredential = new DeviceCodeCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .challengeConsumer(challenge)
                .build();

        _userClient = new GraphServiceClient(_deviceCodeCredential, graphUserScopes);
    }
    // </UserAuthConfigSnippet>

    // <GetUserTokenSnippet>
    public static String getUserToken() throws Exception {
        // Ensure credential isn't null
        if (_deviceCodeCredential == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        final String[] graphUserScopes = _properties.getProperty("app.graphUserScopes").split(",");

        final TokenRequestContext context = new TokenRequestContext();
        context.addScopes(graphUserScopes);

        final AccessToken token = _deviceCodeCredential.getTokenSync(context);
        return token.getToken();
    }
    // </GetUserTokenSnippet>

    // <GetUserSnippet>
    public static User getUser() throws Exception {
        // Ensure client isn't null
        if (_userClient == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        return _userClient.me().get(requestConfig -> {
            requestConfig.queryParameters.select = new String[] {"displayName", "mail", "userPrincipalName"};
        });
    }
    // </GetUserSnippet>

    // <MakeGraphCallSnippet>
    public static void makeGraphCall() {
        // INSERT YOUR CODE HERE
    }
    // </MakeGraphCallSnippet>
}