/*
 * Alex Sing
 * Mr. Stutler
 * 11/3/2023
 *
 * Runner class for DropboxDeduper.
 */

package email.sing.tools.dropbox.deduper;

public class CloudDeduper {

	public static void main(String[] args) throws Exception {
		// Create an app object of DropboxDeduper class.
		GenericFileDeduplicator deduper = new GenericFileDeduplicator();
		// Run init() and run() methods using app object.
		deduper.run();
	}
}