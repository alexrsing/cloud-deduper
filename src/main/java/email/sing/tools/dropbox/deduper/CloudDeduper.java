/*
 * Alex Sing
 * Mr. Stutler
 * 11/3/2023
 *
 * Runner class for GenericFileDeduplicator.
 */

package email.sing.tools.dropbox.deduper;

public class CloudDeduper {

	public static void main(String[] args) throws Exception {
		//Create object of GenericFileDeduplicator class.
		GenericFileDeduplicator deduper = new GenericFileDeduplicator();
		// Run run() method using object.
		deduper.init();
	}
}