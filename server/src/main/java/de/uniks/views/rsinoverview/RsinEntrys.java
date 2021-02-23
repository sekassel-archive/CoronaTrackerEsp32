package de.uniks.views.rsinoverview;

import de.uniks.SQLite.SQLite;
import org.json.JSONArray;

import java.util.List;

public class RsinEntrys {
    private int tekEntrys;
    private String rsin;

    public RsinEntrys() {
    }

    public RsinEntrys(int tekEntrys, String rsin) {
        super();
        this.tekEntrys = tekEntrys;
        this.rsin = rsin;
    }

    public int getTekEntrys() {
        return tekEntrys;
    }

    public void setTekEntrys(int tekEntrys) {
        this.tekEntrys = tekEntrys;
    }

    public String getRsin() {
        return rsin;
    }

    public void setRsin(String rsin) {
        this.rsin = rsin;
    }

    public String getImage() {
        return "https://randomuser.me/api/portraits/men/" + getTekEntrys()
                + ".jpg";
    }

    public String getTekListAsBlock(){
        String tekList = "Empty";
        try{
            List<byte[]> table = SQLite.getRSINTable(Integer.parseInt(rsin));
            tekList = new JSONArray(table).toString();
        } catch(Exception e) {
            tekList = "Error.";
        }
        return tekList;
    }

    @Override
    public int hashCode() {
        return tekEntrys;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RsinEntrys)) {
            return false;
        }
        RsinEntrys other = (RsinEntrys) obj;
        return tekEntrys == other.tekEntrys;
    }

    @Override
    public String toString() {
        return rsin;
    }

    @Override
    public RsinEntrys clone() { //NOSONAR
        try {
            return (RsinEntrys) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(
                    "The Person object could not be cloned.", e);
        }
    }
}
