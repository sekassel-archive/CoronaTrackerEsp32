package de.uniks.views.rsinoverview;

public class RsinEntrys {
    private int id;
    private String name;

    public RsinEntrys() {
    }

    public RsinEntrys(int id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return "https://randomuser.me/api/portraits/men/" + getId()
                + ".jpg";
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RsinEntrys)) {
            return false;
        }
        RsinEntrys other = (RsinEntrys) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return name;
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
