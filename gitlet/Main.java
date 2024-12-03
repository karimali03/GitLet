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
            case "init":
                System.out.println("Repository is already initialized.");
                break;
            case "add":
                if (parameters.length == 0) {
                    System.out.println("No file specified to add.");
                } else {
                    myRepo.add(parameters[0]);
                    saveMyRepo(); // Save repository state after modification
                }
                break;
            case "commit":
                if (parameters.length == 0) {
                    System.out.println("No commit message provided.");
                } else {
                    myRepo.commit(parameters[0]);
                    saveMyRepo(); // Save repository state after modification
                }
                break;
            case "rm" :
                if (parameters.length == 0) {
                    System.out.println("No file specified to remove.");
                }
                else{
                    myRepo.rm(parameters[0]);
                    saveMyRepo();
                }
                break;
            case "log":
                myRepo.log();
                break;
            case "global-log" :
                myRepo.globalLog();
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }
}
