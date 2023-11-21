package email.sing.tools.dropbox.deduper;

public class CloudDeduper {

	public static void main(String[] args) throws Exception {
		// Create an app object of DropboxDeduper class.
		DropboxDeduper deduper = new DropboxDeduper();
		// Run init() and run() mehtods using app object.
		deduper.init();
		deduper.run();
	}
}