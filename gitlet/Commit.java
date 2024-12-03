package gitlet;

import java.util.Date;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;

public class Commit implements Serializable{
    /** The commit message.*/
    private String message;

    /** The date of the commit.*/
    private String timestamp;

    /** A list of strings of hashes of Blobs that are being
     * tracked.*/
    private HashMap<String , String> files;

    /** An array of Hashes of parents. */
    private String[] parents;

    /** The hash of this commit. */
    private String universalID;

    public Commit(String message , HashMap<String , String> files , String[] parents , boolean isIntial){
        this.message = message ;
        this.files = files ;
        this.parents = parents ;

        this.timestamp = isIntial ? new Date().toString() : Utils.DATE_FORMAT.format(new Date());
        this.universalID = this.hashCommit() ;
    }

    public String hashCommit(){
        String tmpFiles ;
        if(this.files == null){
            tmpFiles = "" ;
        }
        else{
            tmpFiles = this.files.toString() ;
        }

        String parents = Arrays.toString(this.parents) ;

        return Utils.sha1(this.message , tmpFiles , this.timestamp , parents) ;
    }

    /** Returns one to create an initial commit easily.*/
    public static Commit initialCommit() {
        return new Commit("initial commit", null, null, false);
    }

    public String getMessage(){
        return this.message;
    }

    public String getTimestamp(){
        return this.timestamp;
    }

    public HashMap<String , String> getFiles(){
        return this.files;
    }

    public String[] getParents(){
        return this.parents;
    }

    public String getParentID(){
        if(this.parents == null){
            return null ;
        }

        return this.parents[0] ;
    }

    public String getUniversalID(){
        return this.universalID;
    }
}
