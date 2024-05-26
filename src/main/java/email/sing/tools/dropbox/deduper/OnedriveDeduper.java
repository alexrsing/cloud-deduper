/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.http.policy.RetryOptions;
import com.azure.identity.*;
import com.microsoft.aad.msal4j.IPublicClientApplication;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OnedriveDeduper {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;
    private static String username;
    private static String password;

    public OnedriveDeduper() throws Exception {
    }

    // Create the authentication provider for the GraphServiceClient and create the GraphServiceClient.
    public static void initializeGraphClient() throws Exception {

        /*
        Properties prop = readPropertiesFile("oAuth.properties");
        final String clientId = prop.getProperty("clientId");
        final List<String> scopes = Arrays.asList(prop.getProperty("app.graphUserScopes")
                .split(","));
        final String tenantId = prop.getProperty("tenantId");
        */

        final String clientId = "c11013f8-0882-4ef6-a7bf-8a06e3d01dcf";
        List<String> scopes = new LinkedList<>();
        scopes.add("user.read");
        scopes.add("profile");
        scopes.add("openid");
        scopes.add("files.readwrite.all");
        final String tenantId = "8e792bc9-49f9-4568-9896-92817f7bd5df";
        String authority = "https://login.microsoftonline.com/organizations";


        TokenRequestContext context = new TokenRequestContext();
        context.setScopes(scopes);

        username = "alexs@singtech.com.au";
        password = "0nT@rget!";

        TokenCredential credential = new UsernamePasswordCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .username(username)
                .password(password)
                .enableUnsafeSupportLogging()
                .authorityHost(authority)
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

    // Retrieve the username and password for the user's Onedrive account.
    public static void getOnedriveLogin() {
        username = JOptionPane.showInputDialog("Please enter your username for your Onedrive account.");
        password = JOptionPane.showInputDialog("Please enter your password for your Onedrive account.");
    }

    // Read the details about Azure App Registration from .properties file.
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
    // Find the files in the Onedrive folder.
    public static DriveItemCollectionResponse getFiles(String startPath, boolean recursive) {
        DriveItemCollectionResponse entries = graphClient.drives()
                .byId("{drive-id}").items()
                .byDriveItemId("{driveItem-id}")
                .children().get();
        return entries;
    }

    public static List<GenericFileMetadata> mapToGenericFiles() {

    }
     */

    // Delete files
    public static void printDisplayName() {
        System.out.println(onedriveUser.displayName);
    }

    // Move files
}