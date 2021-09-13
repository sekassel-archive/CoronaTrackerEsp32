package de.uniks.vaadin.views.viewmodels;

import lombok.Data;
import org.json.JSONArray;

import java.util.Date;
import java.util.List;

@Data
public class RsinEntrys {
    private String rsin;
    private List<byte[]> tekEntries;

    public RsinEntrys(String rsin, List<byte[]> tekEntries) {
        this.rsin = rsin;
        this.tekEntries = tekEntries;
    }

    public int getTekEntriesCount() {
        return tekEntries.size();
    }

    public String getRsinDate() {
        return new Date(new Long(Integer.parseInt(rsin)) * 600 * 1000).toLocaleString().split(",")[0];
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
}
