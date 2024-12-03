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

    /** Overseer of entire tree structure, each branch has a name (String)
     * and a hash ID of its current position so that we can find the commit
     * that it's pointing to.*/
    private HashMap<String, String> branches;

    /** Returns _stagingArea. */
    public HashMap<String, String> getStagingArea() {
        return this.stagingArea;
    }

    /** Returns untrackedFiles. */
    public ArrayList<String> getUntrackedFiles() {
        return this.untrackedFiles;
    }

    /** Returns branches. */
    public HashMap<String, String> getBranches() {
        return this.branches;
    }

    /** Returns the uid of the current head which
     * corresponds to the head branch. */
    public String getHead() {
        return this.branches.get(this.head);
    }

    /*********************** INIT ****************/
    public Repository() {
        Commit initial = Commit.initialCommit() ;
        // Create .gitlet directory
        GITLET_DIR.mkdir() ;
        COMMITS_DIR.mkdir() ;
        STAGING_DIR.mkdir() ;

        String id = initial.getUniversalID();
        File initialFile = join(COMMITS_DIR , id);
        Utils.writeContents(initialFile , Utils.serialize(initial)) ;
        this.head = "master" ;
        this.branches = new HashMap<String , String>();
        this.branches.put("master", id);

        this.stagingArea = new HashMap<String , String>();
        this.untrackedFiles = new ArrayList<String>() ;
    }

    /*********************** ADD FILES ****************/
    public void add(String fileName) {
        File file = new File(fileName);
        if(!file.exists()){
            System.out.println("File does not exist");
            return;
        }

        String fileHash = Utils.sha1(Utils.readContentsAsString(file)) ;
        Commit lastCommit = this.uidToCommit(getHead()) ;
        HashMap<String , String> trackedFiles = lastCommit.getFiles() ;

        File stagingBlob = join(STAGING_DIR, fileHash);
        boolean isTrackedFiles = (trackedFiles != null);

        if(!isTrackedFiles || !trackedFiles.containsKey(fileName) || !trackedFiles.get(fileName).equals(fileHash)){
            this.stagingArea.put(fileName, fileHash);
            String contents = Utils.readContentsAsString(file) ;
            Utils.writeContents(stagingBlob , contents) ;
        }
        else{
            if(stagingBlob.exists()){
                this.stagingArea.remove(fileName);
            }
        }

        if(this.untrackedFiles.contains(fileName)){
            this.untrackedFiles.remove(fileName);
        }

        System.out.println("Added file to staging: " + fileName);
    }

    /*********************** COMMIT ****************/
    public void commit(String msg){
        if(msg.trim().equals("")){
            Utils.message("Please enter a commit message.");
            throw new GitletException();
        }

        Commit lastCommit = this.uidToCommit(getHead()) ;
        HashMap<String , String> trackedFiles = lastCommit.getFiles() ;

        if(trackedFiles == null){
            trackedFiles = new HashMap<String , String>() ;
        }

        if(this.stagingArea.size() != 0 || this.untrackedFiles.size() != 0){
            for (String fileName : this.stagingArea.keySet()) {
                trackedFiles.put(fileName, this.stagingArea.get(fileName));
            }

            for (String fileName : this.untrackedFiles) {
                trackedFiles.remove(fileName);
            }
        }
        else{
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        }

        String[] parent = new String[]{lastCommit.getUniversalID()} ;
        Commit currentCommit = new Commit(msg, trackedFiles, parent, true);
        String id = currentCommit.getUniversalID();
        File newCommFile = join(COMMITS_DIR , id);
        Utils.writeObject(newCommFile, currentCommit);

        this.stagingArea = new HashMap<String, String>();
        this.untrackedFiles = new ArrayList<String>();
        this.branches.put(this.head, id);
    }

    public Commit uidToCommit(String uid) {
        File f = new File(COMMITS_DIR + "/"+uid);
        if (f.exists()) {
            return Utils.readObject(f, Commit.class);
        } else {
            Utils.message("No commit with that id exists.");
            throw new GitletException();
        }
    }

    /*********************** REMOVE ****************/
    public void rm(String fileName) {
        File file = new File(fileName);
        Commit lastCommit = this.uidToCommit(getHead());
        HashMap<String, String> trackedFiles = lastCommit.getFiles();

        if (!file.exists() && (trackedFiles == null || !trackedFiles.containsKey(fileName))) {
            Utils.message("File does not exist.");
            throw new GitletException();
        }

        boolean changed = false;

        // Remove from staging area
        if (this.stagingArea.containsKey(fileName)) {
            String fileHash = this.stagingArea.get(fileName);
            File stagedFile = join(STAGING_DIR, fileHash);
            if (stagedFile.exists()) {
                stagedFile.delete(); // Delete the file from STAGING_DIR
            }
            this.stagingArea.remove(fileName); // Remove from stagingArea map
            changed = true;
        }

        // Mark for untracking if it exists in tracked files
        if (trackedFiles != null && trackedFiles.containsKey(fileName)) {
            this.untrackedFiles.add(fileName); // Add to untrackedFiles list
            File fileToRemove = new File(fileName);
            Utils.restrictedDelete(fileToRemove); // Delete the file from the working directory
            changed = true;
        }

        if (!changed) {
            Utils.message("No reason to remove the file.");
            throw new GitletException();
        }
    }

    /*********************** LOG ****************/
    public void log(){
        String tmpHead = getHead();

        while(tmpHead != null){
            Commit currentCommit = uidToCommit(tmpHead) ;
            printCommit(tmpHead);
            tmpHead = currentCommit.getParentID();
        }
    }

    public  void printCommit(String uid){
        Commit currentCommit = uidToCommit(uid) ;
        System.out.println("===");
        System.out.println("commit " + uid);
        System.out.println("Date: " + currentCommit.getTimestamp());
        System.out.println(currentCommit.getMessage());
        System.out.println();
    }

    /*********************** GLOBAL LOG ****************/
    public void globalLog() {
        File commitFolder = new File(".gitlet/commits");
        File[] commits = commitFolder.listFiles();

        for (File file : commits) {
            printCommit(file.getName());
        }
    }
}