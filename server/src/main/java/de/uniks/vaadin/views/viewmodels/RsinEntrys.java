package de.uniks.vaadin.views.viewmodels;

import org.json.JSONArray;

import java.util.Date;
import java.util.List;

public class RsinEntrys {
    private int tekEntrys;
    private String rsin;
    private List<byte[]> tekEntries;

    public RsinEntrys() {
    }

    public RsinEntrys(String rsin, List<byte[]> tekEntries) {
        super();
        this.tekEntrys = tekEntries.size();
        this.rsin = rsin;
        this.tekEntries = tekEntries;
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

    public String getRsinDate() {
        return new Date(new Long(Integer.parseInt(rsin)) * 600 * 1000).toLocaleString().split(",")[0];
    }

    public void setRsin(String rsin) {
        this.rsin = rsin;
    }

    public String getTekListAsBlock() {
        String tekList = "Empty";
        try {
            tekList = new JSONArray(tekEntries).toString();
        } catch (Exception e) {
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
