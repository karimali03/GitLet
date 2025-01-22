package commitra;

import java.io.Serializable;
import java.util.TreeMap;

import static commitra.Utils.sha1;

public class Commit implements Serializable {

    /** Hash of the current commit.
     */
    private String _ownID;

    /** Hash of the parent commit.
     */
    private String _parentID;

    /** Time when the commit was made.
     */
    private String _timeStamp;

    /** Commit message.
     */
    private String _message;

    /** Contains blobs as
     *  filename and hash.
     *  Hash being the pathname to
     *  the blob.
     */
    private TreeMap<String, String> _objects;

    public Commit(String msg, String pID,
                  TreeMap<String, String> objects, String date) {
        this._timeStamp = date;
        this._parentID = pID;
        this._message = msg;
        this._ownID = sha1(msg, pID, this._timeStamp);
        this._objects = objects;

    }

    public String getOwnID() {
        return this._ownID;
    }

    public String getParentID() {
        return this._parentID;
    }

    public String getMsg() {
        return this._message;
    }

    public String getTimeStamp() {
        return this._timeStamp;
    }

    public TreeMap<String, String> getObjects() {
        return this._objects;
    }

    public void changeOwnID(String hash) {
        this._ownID = hash;
    }

    public String toString() {
        String msg = "=== \n";
        msg += "commit " + getOwnID() + "\n";
        msg += "Date: " + getTimeStamp() + "\n";
        msg += getMsg() + "\n";
        return msg;
    }

}
