/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.graph.requests.DriveItemCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class OnedriveDeduper {

    //private static final String appKey;
    //private static final String appSecret;

    final String clientSecretValue = "iBS8Q~NrO0cMayk~~27iUSi39uII65H7LMV8tcTK";
    final String clientSecret = "iBS8Q~NrO0cMayk~~27iUSi39uII65H7LMV8tcTK";

    final String[] scopes = new String[] { "https://graph.microsoft.com/.default" };

    private static GraphServiceClient graphClient = createOnedriveClient();

    /*
    private static void initializeGraph(Properties properties) {
        try {
            Graph.initializeGraphForUserAuth(properties,
                    challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }
     */

    private static GraphServiceClient createOnedriveClient() throws Exception {
    final String clientId = "YOUR_CLIENT_ID";
    final String tenantId = "YOUR_TENANT_ID";
    final String clientSecret = "YOUR_CLIENT_SECRET";

    final String[] scopes = new String[] { "https://graph.microsoft.com/.default" };

    final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
    .clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build();

    if (null == scopes || null == credential) {
        throw new Exception("Unexpected error");
    }

    final GraphServiceClient graphClient = new GraphServiceClient(credential, scopes);

    return graphClient;
    }

    // Find the files in the Onedrive folder.
    private static DriveItemCollectionResponse findFiles(boolean recursive) {
        DriveItemCollectionResponse result = graphClient.drives().byDriveId("{drive-id}").items().byDriveItemId("{driveItem-id}").children().get();


        /*
        Builder2 clientBuilder = GraphServiceClient.builder()
                .authenticationProvider(_accessTokenAuthProvider);

        IHttpProvider httpProvider = DefaultClientConfig
                .createWithAuthenticationProvider(_accessTokenAuthProvider)
                .getHttpProvider(createOkHttpClientBuilder().build());
        clientBuilder.httpProvider(httpProvider);

        IGraphServiceClient graphServiceClient = clientBuilder.buildClient();
        IDriveItemCollectionPage collectionPage = graphServiceClient.me().drive().root().children().buildRequest()
                .select("Id,Name,Folder,Size").get();

        final Properties oAuthProperties = new Properties();
        try {
            oAuthProperties.load(App.class.getResourceAsStream("oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
        }
         */

        return result;
    }

    // Get access token

    // Delete files

    // Move files
}