package gitlet;

import java.io.File;
import java.util.Arrays;

public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No command provided.");
            return;
        }

        String command = args[0];
        String[] parameters = Arrays.copyOfRange(args, 1, args.length);

        if (Utils.isRepoInitialized()) {
            // Reload the repository from persistent storage
            myRepo = recoverMyRepo();
            runCommand(command, parameters);
        } else {
            if (command.equals("init")) {
                init();
            } else {
                System.out.println("Please initialize the repository first.");
            }
        }
    }

    private static Repository myRepo;
    private static final String ROOTPATH = ".gitlet/repo";

    /** Initializes a new repository. */
    public static void init() {
        myRepo = new Repository();
        System.out.println("Initializing repository...");

        // Save the new repository to persistent storage
        saveMyRepo();

        System.out.println("Repository initialized.");
    }

    /**
     * Reloads the repository from persistent storage.
     */
    public static Repository recoverMyRepo() {
        File root = new File(ROOTPATH);
        if (!root.exists()) {
            throw new IllegalStateException("Repository data not found.");
        }
        return Utils.readObject(root, Repository.class);
    }

    /**
     * Saves the current repository to persistent storage.
     */
    public static void saveMyRepo() {
        File root = new File(ROOTPATH);
        Utils.writeObject(root, myRepo);
    }
    /**
     * Handles command execution.
     */
    public static void runCommand(String command, String[] parameters) {
        switch (command) {
            //command : java gitlet.Main init
            case "init":
                System.out.println("Repository is already initialized.");
                break;
            //command : java gitlet.Main add {file_name}
            case "add":
                if (parameters.length == 0) {
                    System.out.println("No file specified to add.");
                } else {
                    myRepo.add(parameters[0]);
                    saveMyRepo(); // Save repository state after modification
                }
                break;
            //command : java gitlet.Main commit -m {message}
            case "commit":
                if (parameters.length == 0) {
                    System.out.println("No commit message provided.");
                } else {
                    String message = "" ;
                    for(String parameter : parameters) {
                        message += parameter + " ";
                    }

                    myRepo.commit(message);
                    saveMyRepo(); // Save repository state after modification
                }
                break;
            //command : java gitlet.Main rm {file_name}
            case "rm" :
                if (parameters.length == 0) {
                    System.out.println("No file specified to remove.");
                }
                else{
                    myRepo.rm(parameters[0]);
                    saveMyRepo();
                }
                break;
            //command : java gitlet.Main log
            case "log":
                myRepo.log();
                break;
            //command : java gitlet.Main global-log
            case "global-log" :
                myRepo.globalLog();
                break;
            /*
            * 1) command : java gitlet.Main {commit_id} restore {file_name} -> update data of a specific file to that in a specific commit
            * 2) command : java gitlet.Main restore {file_name} -> update data of a specific file to that in the current commit
                   (that if you modify the file but don't commit and want to retain tha data of that file in the last commit)
            * 3) command : java gitlet.Main new {commit_id} -> move from a current commit to another
                   (make the CWD containing all the files in that new commit)
            * */
            case "checkout":
                if (parameters.length == 0) {
                    System.out.println("The input is empty! ERROR!");
                } else {
                    myRepo.checkout(parameters);
                    saveMyRepo(); // Save repository state after modification
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }
}
