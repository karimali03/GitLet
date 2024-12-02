package gitlet;

import java.io.File;
import java.util.Arrays;

public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) { // java gitlet.Main add test.txt
        // Entry point of your program
        if (args.length == 0) {
            System.out.println("No command provided.");
            return;
        }

        // Example of handling a basic command
        String command = args[0];
        String[] prameters = Arrays.copyOfRange(args, 1, args.length);
        if(Utils.isRepoInitialized()){
//            myRepo = recoverMyRepo() ;
            runCommand(command , prameters);
//            File root = new File(ROOTPATH) ;
//            Utils.writeObject(root , myRepo);
        }
        else{
            if(command.equals("init")){
                init();
            }
            else{
                System.out.println("initialize repository first.");
            }

        }
    }

    private  static Repository myRepo;
    private static final String ROOTPATH = ".gitlet/repo";


    private static void init() {
        myRepo = new Repository();
//        File root = new File(ROOTPATH);
//        Utils.writeContents(root, myRepo);
    }

    /**
     * Will do the work of actually saving our information
     * from the repo. It returns the existing repo, assuming
     * that there is one.
     */
    public static Repository recoverMyRepo() {
        File root =  new File(ROOTPATH);
        return Utils.readObject(root, Repository.class);
    }

    private static void runCommand(String command , String[] parameters) {
        switch(command){
            case "init":
                System.out.println("Repo already initialized.");
                break;
            case "add" :
                myRepo.add(parameters[0]);
                break;
        }
    }
}