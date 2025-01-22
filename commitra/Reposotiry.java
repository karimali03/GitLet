package commitra;



import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Objects;

import static commitra.Utils.*;


public class Reposotiry implements Serializable {

    /** Pathname to the current working directory.
     */
    private final File _CWD;

    static final File COMMITRA = new File(".commitra");

    /** Pathname to the Commits directory.
     */
    static final File COMMITS = new File(COMMITRA
            + File.separator + "commits");

    /** Pathname to the Branches directory.
     */
    static final File BRANCHES = new File(COMMITRA
            + File.separator + "branches");

    /** Pathname to the Head Branch text file.
     */
    static final File HEAD_BRANCH = new File(BRANCHES
            + File.separator + "head.txt");

    /** Pathname to the Staging Area text file.
     */
    static final File STAGING_AREA = new File(COMMITRA
            + File.separator + "staging.txt");

    /** Pathname to the Blobs directory.
     */
    static final File OBJECTS = new File(COMMITRA
            + File.separator + "objects");

    /**Pathname to Global Log text file.
     */
    static final File GLOBAL_LOG = new File(COMMITRA
            + File.separator + "global.txt");

    /** Contains the current Head Branch.
     */
    private Branch _headBranch;

    /** Contains the current Head Commit.
     */
    private Commit _head;

    /** Contains the Staging Area.
     */
    private StagingArea _staging;

    public Reposotiry() {
        this._CWD = new File(System.getProperty("user.dir"));
        if (HEAD_BRANCH.exists()) {
            this._headBranch = readObject(HEAD_BRANCH, Branch.class);
            File head = new File(COMMITS + File.separator
                    + this._headBranch.getLastCommit().getOwnID() + ".txt");
            this._head = readObject(head, Commit.class);
        }
        if (STAGING_AREA.exists()) {
            this._staging = readObject(STAGING_AREA, StagingArea.class);
        }

    }

    public void init(String...args) throws IOException {
        if (COMMITRA.exists()) {
            System.out.println("A Commitra version-control system already "
                    + "exists in the current directory.");
            return;
        }
        if (helper(1, args)) {
            return;
        }

        COMMITRA.mkdir();

        Commit initial = new Commit("initial commit", "", new TreeMap<>(),
                "Wed Dec 31 16:00:00 1969 -0800");
        Branch branch = new Branch("master", initial);
        this._head = initial;
        this._headBranch = branch;
        this._staging = new StagingArea();

        String log = initial.toString();
        GLOBAL_LOG.createNewFile();
        writeContents(GLOBAL_LOG, log + "\n");
        OBJECTS.mkdir();
        Utils.join(COMMITRA, "objects");
        COMMITS.mkdir();
        Utils.join(COMMITRA, ".commits");
        BRANCHES.mkdir();
        Utils.join(COMMITRA, ".branches");

        STAGING_AREA.createNewFile();
        writeObject(STAGING_AREA, this._staging);
        Utils.join(COMMITRA, ".staging");

        File masterBranch = new File(BRANCHES
                + File.separator + branch.getName() + ".txt");
        masterBranch.createNewFile();
        writeObject(masterBranch, branch);

        HEAD_BRANCH.createNewFile();
        writeObject(HEAD_BRANCH, branch);

        Utils.join(BRANCHES, File.separator + "head.txt");
        Utils.join(BRANCHES, File.separator + branch.getName() + ".txt");

        File initialCommit = new File(COMMITS
                + File.separator + initial.getOwnID() + ".txt");
        initialCommit.createNewFile();
        writeObject(initialCommit, initial);
        Utils.join(COMMITS, initial.getOwnID());

    }

    public void add(String...args) throws IOException {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        String fileName = args[1];
        File curr = new File(_CWD + File.separator + fileName);
        if (!curr.exists()) {
            System.out.println("File does not exist.");
        } else {
            byte[] contents = readContents(curr);
            String hash = sha1(contents);
            if (getHeadCommit().getObjects().get(fileName) != null
                    && getHeadCommit().getObjects().get(fileName).equals(hash)) {
                if (getStage().getRemoveFiles().contains(fileName)) {
                    getStage().getRemoveFiles().remove(fileName);
                    writeObject(STAGING_AREA, getStage());
                }
            } else {
                getStage().getRemoveFiles().remove(fileName);
                File object = new File(OBJECTS + File.separator + hash + ".txt");
                object.createNewFile(); 
                writeContents(object, contents);
                getStage().add(fileName, hash);
                writeObject(STAGING_AREA, getStage());
            }
        }
    }

    public void commit(String...args) throws IOException {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        if (getStage().getAddFiles().isEmpty()
                && getStage().getRemoveFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        } else if (args[1].equals("")) {
            System.out.println("Please enter a commit message");
            return;
        }

        String msg = args[1];
        @SuppressWarnings("unchecked")
        TreeMap<String, String> newBlobs =
                (TreeMap<String, String>) getHeadCommit().getObjects().clone();

        for (String x : _staging.getRemoveFiles()) {
            newBlobs.remove(x);
        }

        for (String x: getStage().getAddFiles().keySet()) {
            newBlobs.put(x, getStage().getAddFiles().get(x));
        }

        getStage().clear();
        writeObject(STAGING_AREA, getStage());

        SimpleDateFormat formatter =
                new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        Date time = new Date(System.currentTimeMillis());
        String date = formatter.format(time);

        Commit child =
                new Commit(msg, getBranch().getLastCommit().getOwnID(),
                        newBlobs, date);
        this._head = child;
        getBranch().changeLastCommit(child);

        File childCommit = new File(COMMITS
                + File.separator + child.getOwnID() + ".txt");
        childCommit.createNewFile();
        writeObject(childCommit, child);
        writeContents(GLOBAL_LOG,
                readContentsAsString(GLOBAL_LOG) + child + "\n");
        Utils.join(COMMITS, child.getOwnID());

        writeObject(HEAD_BRANCH, getBranch());
        writeObject(new File(BRANCHES + File.separator
                        + getBranch().getName() + ".txt"), getBranch());

    }

    public void switchBranch(String...args) throws IOException {
        if (!commitraExists()) {
            return;
        }

        if(helper(2, args)){
            return;
        }

        String branchName = args[1] + ".txt";
        List<String> files = plainFilenamesIn(BRANCHES);
        File branch = new File(BRANCHES
                + File.separator + branchName);
        if (!files.contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        Branch givenBranch = readObject(branch, Branch.class);
        if (givenBranch.getName().equals(getBranch().getName())) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        _switchBranch(givenBranch);
    }


    public void _switchBranch(Branch givenBranch) throws IOException {
        Branch curr = getBranch();
        List<String> cwdFiles = plainFilenamesIn(_CWD);
        if (!checkIfUntracked(getHeadCommit(), givenBranch.getLastCommit())) {
            return;
        }
        Commit given = givenBranch.getLastCommit();
        assert cwdFiles != null;
        for (String filename : cwdFiles) {
            if (curr.getLastCommit().getObjects().containsKey(filename)
                    && !given.getObjects().containsKey(filename)) {
                File currFile = new File(_CWD + File.separator + filename);
                restrictedDelete(currFile);
            }
        }

        this._headBranch = givenBranch;
        writeObject(HEAD_BRANCH, getBranch());
        writeObject(new File(BRANCHES + File.separator
                + getBranch().getName() + ".txt"), getBranch());
        this._head = getBranch().getLastCommit();

        for (String filename: getHeadCommit().getObjects().keySet()) {
            checkout(filename);
        }

        if (!curr.getName().equals(getBranch().getName())) {
            getStage().clear();
            writeObject(STAGING_AREA, getStage());
        }


    }

    public void checkout(String...args) throws IOException {
        if (!commitraExists()) {
            return;
        }
        if (args.length != 2 && args.length != 3 && args.length != 4) {
            System.out.println("Incorrect Operands.");
        } else if ((args.length == 3 && !args[1].equals("--"))
                || (args.length == 4 && !args[2].equals("--"))) {
            System.out.println("Incorrect Operands.");
            return;
        }

        if (args.length == 3) {
            checkout(args[2]);
        } else if (args.length == 4) {
            checkout(args[3], args[1]);
        } 
    }

    public void checkout(String filename) throws IOException {
        if (!commitraExists()) {
            return;
        }
        if ((getHeadCommit().getObjects() != null)
                && !getHeadCommit().getObjects().containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File headVersion = new File(OBJECTS + File.separator
                + getHeadCommit().getObjects().get(filename) + ".txt");
        File curr = new File(_CWD + File.separator + filename);
        if (!curr.exists()) {
            curr.createNewFile();
        }
        writeContents(curr, readContents(headVersion));

    }

    public void checkout(String filename, String commitID) throws IOException {
        if (!commitraExists()) {
            return;
        }

        commitID = findCommitID(commitID);

        File commit = new File(COMMITS
                + File.separator + commitID);

        if (!commit.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit given = readObject(commit, Commit.class);
        Commit temp = getHeadCommit();
        this._head = given;
        checkout(filename);
        this._head = temp;
    }

    private String findCommitID(String commitID) {
        List<String> commits = plainFilenamesIn(COMMITS);
        commitID = commitID.substring(0, 5);
        assert commits != null;
        for (String file : commits) {
            String curr = file.substring(0, 5);
            if (curr.equals(commitID)) {
                return file;
            }
        }
        return "bad";
    }


    public void remove(String...args) {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        String filename = args[1];
        File curr = new File(_CWD + File.separator + filename);

        if (getHeadCommit().getObjects().containsKey(filename)) {
            restrictedDelete(curr);
            getStage().addRemove(filename);
            getStage().getAddFiles().remove(filename);
            writeObject(STAGING_AREA, getStage());
        } else if (getStage().getAddFiles().containsKey(filename)) {
            getStage().getAddFiles().remove(filename);
            writeObject(STAGING_AREA, getStage());
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    public void log(String...args) {
        if (!commitraExists()) {
            return;
        }
        if (helper(1, args)) {
            return;
        }
        Commit curr = getHeadCommit();
        while (curr != null) {
            System.out.println(curr);
            File nextCommit = new File(COMMITS + File.separator
                    + curr.getParentID() + ".txt");
            if (nextCommit.exists()) {
                curr = readObject(nextCommit, Commit.class);
            } else {
                curr = null;
            }
        }
    }

    public void globalLog(String...args) {
        if (!commitraExists()) {
            return;
        }
        if (helper(1, args)) {
            return;
        }
        System.out.println(readContentsAsString(GLOBAL_LOG));
    }

    public void find(String...args) {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        List<String> commitNames = plainFilenamesIn(COMMITS);
        int count = 0;
        String message = args[1];
        assert commitNames != null;
        for (String commit: commitNames) {
            Commit curr = readObject(new File(COMMITS + File.separator
                    + commit), Commit.class);
            if (curr.getMsg().equals(message)) {
                count++;
                System.out.println(curr.getOwnID());
            }
        }

        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }

    }

    public void status(String... args) { 
        if (!commitraExists()) {
            return;
        }
        if (helper(1, args)) {
            return;
        }
    
        List<String> branches = plainFilenamesIn(BRANCHES);
        Collections.sort(branches);
    
        List<String> addFiles = new ArrayList<>();
        addFiles.addAll(getStage().getAddFiles().keySet());
        Collections.sort(addFiles);
    
        List<String> removeFiles = new ArrayList<>();
        removeFiles.addAll(getStage().getRemoveFiles());
        Collections.sort(removeFiles);
    
        // Get the current branch and its commit
        Branch currentBranch = getBranch();

        // Get files that are modified but not staged
        List<String> modifiedFilesNotStaged = getModifications();

        // Get untracked files (files in CWD but not in head commit)
        List<String> untrackedFiles = getUntrackedFilesCWD();

        // Print the status output
        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranch.getName());
        for (String branch : branches) {
            Branch curr = readObject(new File(BRANCHES + File.separator + branch), Branch.class);
            if (!curr.getName().equals(currentBranch.getName())) {
                System.out.println(curr.getName());
            }
        }
        
        // Staged Files
        System.out.println("=== Staged Files ===");
        for (String filename : addFiles) {
            System.out.println(filename);
        }
        
        // Removed Files
        System.out.println("=== Removed Files ===");
        for (String filename : removeFiles) {
            System.out.println(filename);
        }
    
        // Modifications Not Staged For Commit
        System.out.println("=== Modifications Not Staged For Commit ===");
        Collections.sort(modifiedFilesNotStaged);
        for (String filename : modifiedFilesNotStaged) {
            System.out.println(filename);
        }
    
        // Untracked Files
        System.out.println("=== Untracked Files ===");
        Collections.sort(untrackedFiles);
        for (String filename : untrackedFiles) {
            System.out.println(filename);
        }
    }   

    public ArrayList<String> getUntrackedFilesCWD() {
        ArrayList<String> untracked = new ArrayList<>();
        File[] cwdFiles = _CWD.listFiles();
    
        for (File file : cwdFiles) {
            String fileName = file.getName();
            boolean isTracked = false;
    
            // Check if file is tracked
            Commit lastCommit = getHeadCommit();
            if (lastCommit.getObjects() != null && lastCommit.getObjects().containsKey(fileName)) {
                isTracked = true;
            }
    
            // Check if file is staged
            if (_staging.getAddFiles().containsKey(fileName)) {
                isTracked = true;
            }
    
            // If not tracked or staged, add to untracked
            if (!isTracked && !file.isDirectory()) {
                untracked.add(fileName);
            }
        }
        
        return untracked;
    }
    

    public ArrayList<String> getModifications() {
        ArrayList<String> modifications = new ArrayList<>();
        // check modifications in staging area
        for (String fileName : _staging.getAddFiles().keySet()) {
            File file = new File(_CWD + File.separator + fileName);
            String stagedHash = _staging.getAddFiles().get(fileName);
    
            if (!file.exists()) {
                modifications.add(fileName + " (deleted)");
            } else {
                String currentHash = Utils.sha1(Utils.readContents(file));
                StringBuilder sh = new StringBuilder(stagedHash);
                StringBuilder ch = new StringBuilder(currentHash);
                if (!sh.toString().equals(ch.toString())) {
                    modifications.add(fileName + " (modified)");
                }
            }
        }
        ArrayList<String> removed_file = _staging.getRemoveFiles();
        for(String fileName : removed_file){
            File file = new File(_CWD + File.separator + fileName);
            if(file.exists()){
                modifications.add(fileName + " (added)");
            }
        }

        // check modifications in the tracked files
        Commit lastCommit = getHeadCommit();
        if (lastCommit.getObjects() != null) {
            for (String fileName : lastCommit.getObjects().keySet()) {
                File file = new File(_CWD + File.separator + fileName);
                if (!file.exists()) {
                   if(!removed_file.contains(fileName))
                    modifications.add(fileName + " (deleted)");
                } else {
                    String currentHash = Utils.sha1(Utils.readContents(file));
                    String lastCommitHash = lastCommit.getObjects().get(fileName);
                    StringBuilder lh = new StringBuilder(lastCommitHash);
                    StringBuilder ch = new StringBuilder(currentHash);
                    if (!lh.toString().equals(ch.toString()) && !_staging.getAddFiles().containsKey(fileName)) {
                        modifications.add(fileName + " (modified)");
                    }
                }
            }
        }
        
        return modifications;
    }    

    public void branch(String...args) throws IOException {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        String name = args[1];
        File newBranchFile = new File(BRANCHES + File.separator
                + name + ".txt");
        if (newBranchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        newBranchFile.createNewFile();
        Branch newBranch = new Branch(name, getHeadCommit());
        writeObject(newBranchFile, newBranch);
    }

    public void removeBranch(String...args) {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        String filename = args[1] + ".txt";
        File branch = new File(BRANCHES
                + File.separator + filename);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        Branch thisBranch = readObject(branch, Branch.class);
        if (thisBranch.getName().equals(getBranch().getName())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branch.delete();
    }

    public void reset(String...args) throws IOException {
        if (!commitraExists()) {
            return;
        }
        if (helper(2, args)) {
            return;
        }
        String commitID = args[1];
        File thisCommit = new File(COMMITS + File.separator
                + commitID + ".txt");
        if (!thisCommit.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit replacement = readObject(thisCommit, Commit.class);
        if (!checkIfUntracked(getHeadCommit(), replacement)) {
            return;
        }
        for (String file: Objects.requireNonNull(plainFilenamesIn(_CWD))) {
            if (getHeadCommit().getObjects().containsKey(file)
                    && !replacement.getObjects().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }
        getBranch().changeLastCommit(replacement);
        writeObject(HEAD_BRANCH, getBranch());
        writeObject(new File(BRANCHES + File.separator
                + getBranch().getName() + ".txt"), getBranch());
        this._head = replacement;
        for (String filename : replacement.getObjects().keySet()) {
            checkout(filename);
        }
        getStage().clear();
        writeObject(STAGING_AREA, getStage());
    }

    public void merge(String...args) throws IOException {
        if ((!commitraExists()) || helper(2, args)
                || mergeErrors(args[1])) {
            return;
        }
        File givenBranch = new File(BRANCHES
                + File.separator + args[1] + ".txt");
        Commit head = getHeadCommit();
        Commit given = readObject(givenBranch, Branch.class).getLastCommit();
        Commit split = splitPoint(head, given);
        Boolean conf = false;
        for (String file: allFiles(head, split, given)) {
            String hHash = head.getObjects().get(file);
            String gHash = given.getObjects().get(file);
            String sHash = split.getObjects().get(file);
            boolean conflict = false;
            if (head.getObjects().containsKey(file)
                    && given.getObjects().containsKey(file)) {
                conflict = !hHash.equals(sHash) && !gHash.equals(sHash)
                        && !hHash.equals(gHash);
            }

            if (split.getObjects().containsKey(file)) {
                if (conflict) {
                    mergeHelper1(hHash, file, gHash);
                    conf = true;
                } else if (head.getObjects().containsKey(file)
                        && hHash.equals(sHash)
                        && !given.getObjects().containsKey(file)) {
                    remove("remove", file);
                } else if (given.getObjects().containsKey(file)
                        && !gHash.equals(sHash) && sHash.equals(hHash)) {
                    checkout(file, given.getOwnID());
                } else if (given.getObjects().containsKey(file)
                        && gHash.equals(sHash)
                        && !head.getObjects().containsKey(file)) {
                    getStage().addRemove(file);
                }
            } else {
                if (conflict) {
                    mergeHelper1(hHash, file, gHash);
                    conf = true;
                } else if (given.getObjects().containsKey(file)
                        && !head.getObjects().containsKey(file)) {
                    checkout(file, given.getOwnID());
                    add("add", file);
                }
            }
        }
        if (conf) {
            System.out.println("Encountered a merge conflict.");
            return;
        } else {
            String message = "Merged " + args[1]
                    + " into " + getBranch().getName() + ".";
            mergeCommit(message, given);
        }
    }

    public ArrayList<String> allFiles(Commit c, Commit s, Commit g) {
        ArrayList<String> allFiles = new ArrayList<>(c.getObjects().keySet());

        for (String file : s.getObjects().keySet()) {
            if (!allFiles.contains(file)) {
                allFiles.add(file);
            }
        }

        for (String file : g.getObjects().keySet()) {
            if (!allFiles.contains(file)) {
                allFiles.add(file);
            }
        }

        return allFiles;
    }

    public ArrayList<String> p2Commits(String p2) {
        File parent = new File(COMMITS + File.separator
                + p2 + ".txt");
        Commit parent2 = readObject(parent, Commit.class);
        ArrayList<String> ancestors = new ArrayList<>();

        while (parent2 != null) {
            ancestors.add(parent2.getOwnID());
            parent = new File(COMMITS + File.separator
                    + parent2.getParentID() + ".txt");
            if (parent.exists()) {
                parent2 = readObject(parent, Commit.class);
            } else {
                parent2 = null;
            }
        }

        return ancestors;
    }

    public void mergeHelper1(String hBlob, String file, String gBlob) {
        File merge = new File(_CWD + File.separator + file);
        File head = new File(OBJECTS + File.separator + hBlob + ".txt");
        File given = new File(OBJECTS + File.separator + gBlob + ".txt");
        String contents = "<<<<<<< HEAD\n";
        if (head.exists()) {
            contents += readContentsAsString(head);
        }
        contents += "=======\n";
        if (given.exists()) {
            contents += readContentsAsString(given);
        }
        contents += ">>>>>>>";
        writeContents(merge, contents);
    }

    public boolean mergeErrors(String branch) throws IOException {
        File givenBranch = new File(BRANCHES
                + File.separator + branch + ".txt");
        if (!givenBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        Commit head = getHeadCommit();
        Commit given = readObject(givenBranch, Branch.class).getLastCommit();
        Commit split = splitPoint(head, given);
        ArrayList<String> ancestryHead = ancestorList(head);
        if (!checkIfUntracked(head, given)) {
            return true;
        }

        if (!getStage().getRemoveFiles().isEmpty()
                || !getStage().getAddFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (readObject(givenBranch, Branch.class).
                getName().equals(getBranch().getName())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }

        if (ancestryHead.contains(given.getOwnID())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return true;
        } else if (split.equals(head)) {
            _switchBranch(readObject(givenBranch, Branch.class));
            System.out.println("Current branch fast-forwarded.");
            return true;
        }

        return false;
    }

    public Commit splitPoint(Commit head, Commit given) {
        Commit split = null;
        ArrayList<String> commitList = ancestorList(head);

        for (String commit: ancestorList(given)) {
            if (commitList.contains(commit)) {
                File s = new File(COMMITS + File.separator
                        + commit + ".txt");
                split = readObject(s, Commit.class);
                break;
            }
        }

        return split;
    }

    public ArrayList<String> ancestorList(Commit current) {
        ArrayList<String> commitList = new ArrayList<>();

        while (current != null && readObject(new File(COMMITS
                + File.separator
                + current.getOwnID() + ".txt"), Commit.class) != null) {

            commitList.add(current.getOwnID());
            if (current instanceof MergeCommit) {
                File p2 = new File (COMMITS + File.separator
                        + ((MergeCommit) current).getParent2() + ".txt");
                Commit parent = readObject(p2, Commit.class);
                commitList.addAll(ancestorList(parent));
            }
            if (!current.getParentID().equals("")) {
                current = readObject(new File(COMMITS
                        + File.separator
                        + current.getParentID() + ".txt"), Commit.class);
            } else {
                current = null;
            }
        }

        return commitList;
    }


    public boolean checkIfUntracked(Commit head, Commit given) {
        List<String> cwdFiles = plainFilenamesIn(_CWD);
        assert cwdFiles != null;
        for (String filename : cwdFiles) {
            if (!head.getObjects().containsKey(filename)
                    && given.getObjects().containsKey(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return false;
            }
        }
        return true;
    }

    public void mergeCommit(String msg, Commit parent2) throws IOException {
        @SuppressWarnings("unchecked")
        TreeMap<String, String> newBlobs =
                (TreeMap<String, String>) getHeadCommit().getObjects().clone();

        for (String filename : parent2.getObjects().keySet()) {
            if (!newBlobs.containsKey(filename)) {
                newBlobs.put(filename, parent2.getObjects().get(filename));
            }
        }

        for (String x : _staging.getRemoveFiles()) {
            newBlobs.remove(x);
        }

        for (String x: getStage().getAddFiles().keySet()) {
            newBlobs.put(x, getStage().getAddFiles().get(x));
        }

        getStage().clear();
        writeObject(STAGING_AREA, getStage());

        SimpleDateFormat formatter =
                new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        Date time = new Date(System.currentTimeMillis());
        String date = formatter.format(time);

        MergeCommit child = new MergeCommit(msg, getHeadCommit().getOwnID(),
                parent2.getOwnID(), newBlobs, date);
        this._head = child;
        getBranch().changeLastCommit(child);

        File childCommit = new File(COMMITS + File.separator
                + child.getOwnID() + ".txt");
        childCommit.createNewFile();
        writeObject(childCommit, child);
        Utils.join(COMMITS, child.getOwnID());

        writeObject(HEAD_BRANCH, getBranch());
        writeObject(
                new File(BRANCHES + File.separator
                        + getBranch().getName() + ".txt"), getBranch());

    }

    static boolean helper(int size, String...args) {
        if (args.length == size) {
            return false;
        } else {
            System.out.println("Incorrect Operands.");
            return true;
        }
    }

    static boolean commitraExists() {
        if (!COMMITRA.exists()) {
            System.out.println("Not in an initialized Commitra directory.");
            return false;
        }
        return true;
    }

    public Commit getHeadCommit() {
        return this._head;
    }

    public Branch getBranch() {
        return this._headBranch;
    }

    public StagingArea getStage() {
        return this._staging;
    }


}
