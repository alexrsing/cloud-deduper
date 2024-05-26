/*
 * Alex Sing
 * Mr. Stutler
 * 4/22/2024
 *
 * OneDriveDeduper retrieves FileMetadata from the user's OneDrive account and uses data to create CommonFileMetadata object.
 */

package email.sing.tools.dropbox.deduper;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.UsernamePasswordCredentialBuilder;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class OnedriveDeduper {

    public static GraphServiceClient graphClient;
    public static User onedriveUser;
    private static String username;
    private static String password;

    //Azure App Properties
    private static final String clientId = "c11013f8-0882-4ef6-a7bf-8a06e3d01dcf";
    private static final String tenantId = "8e792bc9-49f9-4568-9896-92817f7bd5df";

    public OnedriveDeduper() throws Exception {
    }


    // Create the authentication provider for the GraphServiceClient and create the GraphServiceClient.
    public static void initializeGraphClientByPassword() throws Exception {

        /*
        Properties prop = readPropertiesFile("oAuth.properties");
        final String clientId = prop.getProperty("clientId");
        final List<String> scopes = Arrays.asList(prop.getProperty("app.graphUserScopes")
                .split(","));
        final String tenantId = prop.getProperty("tenantId");

         */

        List<String> scopes = new LinkedList<>();
        scopes.add("user.read");
        scopes.add("profile");
        scopes.add("openid");
        scopes.add("files.readwrite.all");
        String authority = "https://login.microsoftonline.com/organizations";

        //TokenRequestContext context = new TokenRequestContext();
        //context.setScopes(scopes);

        username = "alexs@singtech.com.au";
        password = "0nT@rget!";

        TokenCredential credential = new UsernamePasswordCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .username(username)
                .password(password)
                .build();


        if (null == scopes || null == credential) {
            throw new Exception("Unexpected error");
        }

        graphClient = new GraphServiceClient(credential, Arrays.toString(scopes.toArray()));

        /*
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
         */

        onedriveUser = graphClient.me().get();
        }

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
        System.out.println(onedriveUser.getDisplayName());
    }

    // Move files
}