/*
 *  Alex Sing
 *  Mr. Stutler
 *  11/3/2023
 *
 * DropboxDeduper finds files and uses the data to create GenericFileMetadata objects.
 *
 */

package email.sing.tools.dropbox.deduper;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;

import java.awt.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class DropboxDeduper implements DedupeFileAccessor {

	//public static String startPath;
    private static final String ACCESS_TOKEN;

	static {
		try {
			ACCESS_TOKEN = getDropboxAccessToken();
		} catch (DbxException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
    }

	private static final String appKey = "69rotl1nmd80etf";
	private static final String appSecret = "nn93eef34q21gtf";
	private static DbxClientV2 dropboxClient; // Client used to send requests to Dropbox

	public void init() {
		dropboxClient = getDropboxClient();
	}


	/*
	 * Creates a new Dropbox client to make remote calls to the Dropbox API user endpoints
	 */
    public static DbxClientV2 getDropboxClient() {
        System.out.println("Using Access Token: " + ACCESS_TOKEN);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/deduper").build();
        return new DbxClientV2(config, ACCESS_TOKEN);
    }

	/*
	 * Gets access token that is used in the DbxClientV2
	 */
	private static String getDropboxAccessToken() throws DbxException, URISyntaxException {
		DbxRequestConfig requestConfig = new DbxRequestConfig("dropbox/deduper");
		DbxAppInfo appInfo = new DbxAppInfo(appKey, appSecret);
		DbxWebAuth auth = new DbxWebAuth(requestConfig, appInfo);

		DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build();
		String authorizeUrl = auth.authorize(authRequest);

		String accessCode = getDropboxAccessCode(authorizeUrl);

		if (accessCode != null) {
			accessCode = accessCode.trim();
			DbxAuthFinish authFinish = auth.finishFromCode(accessCode);
			return authFinish.getAccessToken();
		}
		return "";
	}

	/*
	 * Display the URL with directions to get access code.
	 */
	private static String getDropboxAccessCode(String url) {
		/*
		JLabel label = new JLabel();
		Font font = label.getFont();

		// create some css from the label's font
		StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
		style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
		style.append("font-size:").append(font.getSize()).append("pt;");

		// Dialog content with html
		JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">"
				+ "1. Go to <a href=\"" + url + "\">" + url + "</a><br />"
				+ "2. Click \"Allow\"<br />3. Copy the authorization code <br />4. Enter the authorization code here</body></html>");

		// Handle link events
		ep.addHyperlinkListener(new HyperlinkListener()
		{
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e)
			{
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException | URISyntaxException ex) {
						throw new RuntimeException(ex);
					}
                }
			}
		});
		ep.setEditable(false);
		ep.setBackground(label.getBackground());
		 */

		System.out.println("Go to " + url + " and click allow and copy the authorization code.");

		Scanner scan = new Scanner(System.in);
		String accessCode = scan.next();

		return accessCode;
		// Show dialog box
        //return JOptionPane.showInputDialog(null, ep, "File De-duplicator", JOptionPane.OK_CANCEL_OPTION);
	}

	/*
	 * Return a list of all files in the path.
	 * If recursive is true, include all the files within sub-folders of the path.
	 */
	public List<GenericFileMetadata> getFiles(String path, boolean recursive) throws Exception {
	    ListFolderResult result = dropboxClient.files().listFolderBuilder(path)
	         .withRecursive(recursive)
	         .start();

	    List<Metadata> entries = result.getEntries();
		while (result.getHasMore()) {
		    result = dropboxClient.files().listFolderContinue(result.getCursor());
		    entries.addAll(result.getEntries());
		}

		removeFolders(entries);

		return mapToGenericFiles(entries);
	}

	/*
	 * Create a map of the duplicate files.
	 */
	public Map<String, List<GenericFileMetadata>> populateMap(List<GenericFileMetadata> files) {
		Map<String, List<GenericFileMetadata>> fileMap = new HashMap<>();
		GenericFileDeduplicator.totalFileCount = files.size();

		for (GenericFileMetadata f : files) {
			String fileHash = f.getContentHash();
			if (fileMap.containsKey(fileHash)) {
				fileMap.get(fileHash).add(f);
			}
			else {
				List<GenericFileMetadata> list = new LinkedList<>();
				list.add(f);
				fileMap.put(fileHash, list);
			}
		}

		return fileMap;
	}

	/*
	 * Map Metadata objects to GenericFileMetadata objects using required data.
	 */
	public static List<GenericFileMetadata> mapToGenericFiles(List<Metadata> files) {
		List<GenericFileMetadata> genericFiles = new ArrayList<>();
		return files.stream()
				.map(file -> new GenericFileMetadata(file.getName(), file.getPathLower(), ((FileMetadata) file).getContentHash(), ((FileMetadata) file).getSize()))
				.toList();
	}

	private static void removeFolders(List<Metadata> list) {
		Iterator<Metadata> it = list.iterator();
		while (it.hasNext()) {
			Metadata item = it.next();
			if (item instanceof FolderMetadata) {
				it.remove();
			}
		}
	}

	/*
	 * Create a new folder to move duplicate files to.
	 */
	private void createNewFolder() {
		String folderName = getFolderName();
		try {
			dropboxClient.files().createFolderV2(folderName);
		}
		catch (Exception e) {
			System.err.println("Error creating folder: " + e);
		}
	}

	/*
	 * Appends the current date to the duplicate folder name for a specific name.
	 */
	private static String getFolderName() {
		return "/Duplicate Files Folder - " + GenericFileDeduplicator.getCurrentDate();
	}

	/*
	 * Move files to the new folder that is created.
	 */
	@Override
	public void moveFilesToFolder(Map<String, List<GenericFileMetadata>> map) throws Exception {
		String newFolderName = getFolderName();
		createNewFolder();

		for (String key : map.keySet()) {
			for (GenericFileMetadata file : map.get(key)) {
				String start = file.getFileRoot();
				String destination = newFolderName;
				try {
					MoveV2Builder moveV2Builder = dropboxClient.files().moveV2Builder(start, destination)
							.withAllowSharedFolder(true)
							.withAllowOwnershipTransfer(true)
							.withAutorename(true);
					moveV2Builder.start();
					Thread.sleep(600);
				}
				catch (RelocationErrorException e) {
					try {
						MoveV2Builder moveV2Builder = dropboxClient.files().moveV2Builder(start, destination)
								.withAllowSharedFolder(true)
								.withAllowOwnershipTransfer(true)
								.withAutorename(true);
						moveV2Builder.start();
					} catch (RelocationErrorException er) {
						System.err.println("File could not be moved");
						throw new RuntimeException(er);
					}
				}
			}
		}
	}

	/*
	 * Delete all files in the "Duplicate Files" folder, must have moved them into the folder first.
	 */
	@Override
	public void deleteFiles(Map<String, List<GenericFileMetadata>> map) {
		for (String key : map.keySet()) {
			for (GenericFileMetadata file : map.get(key)) {
				try {
					dropboxClient.files().deleteV2(file.getFileRoot());
					Thread.sleep(600);
				} catch (Exception e) {
					System.err.println("Error deleting file: " + e);
				}
			}
		}
	}

	/*
	 * Move list to its own file without moving the files.
	 */
	@Override
	public void uploadLogFile(File logFile) {
		try (InputStream in = new FileInputStream(logFile.getName())) {
				dropboxClient.files().uploadBuilder("/" + logFile.getName())
						.uploadAndFinish(in);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}