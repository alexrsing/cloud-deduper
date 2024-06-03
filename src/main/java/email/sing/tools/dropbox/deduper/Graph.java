/*
 * Alex Sing
 * Mr. Stutler
 * 5/8/2024
 *
 * Graph.java creates the client, housed in OnedriveDeduper, that will make calls to the Microsoft Graph API.
 */

package email.sing.tools.dropbox.deduper;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.function.Consumer;

public class Graph {

    private static DeviceCodeCredential deviceCodeCredential;

    private static final String clientId = "c11013f8-0882-4ef6-a7bf-8a06e3d01dcf";
    private static final String[] graphUserScopes = { "user.read", "profile", "openid", "files.readwrite.all", "application.readwrite.all" };
    private static final String tenantId = "common";

    public static void initializeGraphForUserAuth(Consumer<DeviceCodeInfo> challenge) {
        deviceCodeCredential = new DeviceCodeCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .challengeConsumer(challenge)
                .build();

        OnedriveDeduper.graphClient = new GraphServiceClient(deviceCodeCredential, graphUserScopes);
        OnedriveDeduper.onedriveUser = OnedriveDeduper.graphClient.me().get();

    }
}