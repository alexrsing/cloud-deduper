package email.sing.tools.dropbox.deduper;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;

import okhttp3.Request;

public class Graph {
    private static Properties _properties;
    private static DeviceCodeCredential _deviceCodeCredential;
    private static GraphServiceClient<Request> _userClient;

    public static void initializeGraphForUserAuth(Properties properties, Consumer<DeviceCodeInfo> challenge) throws Exception {
        // Ensure properties isn't null
        if (properties == null) {
            throw new Exception("Properties cannot be null");
        }

        _properties = properties;

        final String clientId = properties.getProperty("app.clientId");
        final String tenantId = properties.getProperty("app.tenantId");
        final List<String> graphUserScopes = Arrays
                .asList(properties.getProperty("app.graphUserScopes").split(","));

        _deviceCodeCredential = new DeviceCodeCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .challengeConsumer(challenge)
                .build();

        final TokenCredentialAuthProvider authProvider =
                new TokenCredentialAuthProvider(graphUserScopes, _deviceCodeCredential);

        _userClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }

    public static String getUserToken() throws Exception {
        // Ensure credential isn't null
        if (_deviceCodeCredential == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        final String[] graphUserScopes = _properties.getProperty("app.graphUserScopes").split(",");

        final TokenRequestContext context = new TokenRequestContext();
        context.addScopes(graphUserScopes);

        final AccessToken token = _deviceCodeCredential.getToken(context).block();
        return token.getToken();
    }

    private static void displayAccessToken() {
        try {
            final String accessToken = Graph.getUserToken();
            System.out.println("Access token: " + accessToken);
        } catch (Exception e) {
            System.out.println("Error getting access token");
            System.out.println(e.getMessage());
        }
    }

    public static User getUser() throws Exception {
        // Ensure client isn't null
        if (_userClient == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        return _userClient.me()
                .buildRequest()
                .select("displayName,mail,userPrincipalName")
                .get();
    }
}
