package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static gitlet.Utils.join;

public class Repository implements Serializable {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, "/.gitlet");

    public  static  final File COMMITS_DIR = join(CWD, "/.gitlet/commits");
    public  static  final File STAGING_DIR = join(CWD, "/.gitlet/staging");

    private String head;

    /** Staging Area, maps the name of the file, useful for figuring out
     * whether we need to swap it out for existing file in commit, or add
     * it entirely new. */
    private HashMap<String, String> stagingArea;

    /** Untracked files are like the opposite of the Staging Area,
     * these are files that WERE tracked before, and now, for the
     * next commit, they're not going to be added. */
    private ArrayList<String> untrackedFiles;

    /** Returns _stagingArea. */
    public HashMap<String, String> getStagingArea() {
        return this.stagingArea;
    }

    /** Returns untrackedFiles. */
    public ArrayList<String> getUntrackedFiles() {
        return this.untrackedFiles;
    }

    /*********************** INIT ****************/
    public Repository() {
        // Create .gitlet directory
        GITLET_DIR.mkdir() ;
        COMMITS_DIR.mkdir() ;
        STAGING_DIR.mkdir() ;
    }

    /*********************** ADD FILES ****************/
    public static void add(String fileName) {
        File file = new File(fileName);
        if(!file.exists()){
            System.out.println("File does not exist");
            return;
        }

        String fileHash = Utils.sha1(Utils.readContentsAsString(file)) ;

        File stagingBlob = join(STAGING_DIR, fileHash);

        String contents = Utils.readContentsAsString(file) ;
        Utils.writeContents(stagingBlob, contents) ;

        System.out.println("Added file to staging: " + fileName);
    }
}