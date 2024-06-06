package email.sing.tools.dropbox.deduper;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface DedupeFileAccessor {

    void init();

    List<GenericFileMetadata> getFiles(String path, boolean recursive) throws Exception;

    void moveFilesToFolder(Map<String, List<GenericFileMetadata>> files) throws Exception;

    void deleteFiles(Map<String, List<GenericFileMetadata>> map);

    void uploadLogFile(File file);

    String createNewFolder();
}
