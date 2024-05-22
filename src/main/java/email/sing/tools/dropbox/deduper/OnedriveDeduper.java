/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.*;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.nimbusds.oauth2.sdk.token.Token;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class OnedriveDeduper {

    public static GraphServiceClient graphClient;

    public static User onedriveUser;

    private static String username;

    private static String password;

    public OnedriveDeduper() throws Exception {
    }

    public static void createGraphClient() throws Exception {
        Properties prop = readPropertiesFile("oAuth.properties");
        final String clientId = prop.getProperty("clientId");
        final List<String> scopes = Arrays.asList(prop.getProperty("app.graphUserScopes").split(","));
        final String tenantId = prop.getProperty("tenantId");

        TokenRequestContext context = new TokenRequestContext();
        context.setScopes(scopes);

        TokenCredential credential = new UsernamePasswordCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .username(username)
                .password(password)
                .build();

        if (null == scopes || null == credential) {
            throw new Exception("Unexpected error");
        }

        graphClient = GraphServiceClient.builder()
                .authenticationProvider(new IAuthenticationProvider() {
                    @NotNull
                    @Override
                    public CompletableFuture<String> getAuthorizationTokenAsync(@NotNull URL requestUrl) {
                        CompletableFuture<String> future = new CompletableFuture<>();
                        future.complete(credential.getTokenSync(context).getToken());
                        return future;
                    }
                })
                .buildClient();

        onedriveUser = graphClient.me().buildRequest().get();
    }

    public static void getOnedriveLogin() {
        username = JOptionPane.showInputDialog("Please enter your username for your Onedrive account.");
        password = JOptionPane.showInputDialog("Please enter your password for your Onedrive account.");
    }

    // Read the details about Azure App from .properties file.
    public static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = null;
        try {
            fis = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            fis.close();
        }
        return prop;
    }

    /*
    static void initializeGraph(Properties properties) {
        try {
            Graph.initializeGraphForUserAuth(properties,
                    challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }

    // Find the files in the Onedrive folder.
    public static DriveItemCollectionResponse getFiles(String startPath, boolean recursive) {
        DriveItemCollectionResponse result = graphClient.drives()
                .byId("{drive-id}").items()
                .byDriveItemId("{driveItem-id}")
                .children().get();
        return result;
    }

    public static List<GenericFileMetadata> mapToGenericFiles() {

    }
     */

    // Delete files

    // Move files
}