package gitlet;

import java.io.IOException;

public class Main {

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        
        Reposotiry repo = new Reposotiry(args[args.length-1]);
        
        switch (args[0]) {
            case "init":
                repo.init(args);
                break;
            case "add":
                repo.add(args);
                break;
            case "commit":
                repo.commit(args);
                break;
            case "rm":
                repo.remove(args);
                break;
            case "log":
                repo.log(args);
                break;
            case "global-log":
                repo.globalLog(args);
                break;
            case "find":
                repo.find(args);
                break;
            case "status":
                repo.status(args);
                break;
            case "checkout":
                repo.checkout(args);
                break;
            case "branch":
                repo.branch(args);
                break;
            case "rm-branch":
                repo.removeBranch(args);
                break;
            case "reset":
                repo.reset(args);
                break;
            case "merge":
                repo.merge(args);
                break;
            default:
                System.out.println("No command with that name exist.");
        }
    }
}
