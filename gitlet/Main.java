package gitlet;

import java.io.IOException;

public class Main {

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        Reposotiry repo = new Reposotiry(args[args.length-1]);
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 0, newArgs, 0, args.length - 1);
        
        switch (args[0]) {
            case "init":
                repo.init(newArgs);
                break;
            case "add":
                repo.add(newArgs);
                break;
            case "commit":
                repo.commit(newArgs);
                break;
            case "rm":
                repo.remove(newArgs);
                break;
            case "log":
                repo.log(newArgs);
                break;
            case "global-log":
                repo.globalLog(newArgs);
                break;
            case "find":
                repo.find(newArgs);
                break;
            case "status":
                repo.status(newArgs);
                break;
            case "checkout":
                repo.checkout(newArgs);
                break;
            case "branch":
                repo.branch(newArgs);
                break;
            case "rm-branch":
                repo.removeBranch(newArgs);
                break;
            case "reset":
                repo.reset(newArgs);
                break;
            case "merge":
                repo.merge(newArgs);
                break;
            default:
                System.out.println("No command with that name exist.");
        }
    }
}
