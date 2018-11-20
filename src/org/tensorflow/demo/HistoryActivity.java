package org.tensorflow.demo;

public class HistoryActivity {
    private int Id;
    private String Name;
    private int Image;
    private String Time;
    private String Description;

    public HistoryActivity() {
    }

    public HistoryActivity(int id, String name, int image, String time, String description) {
        Id = id;
        Name = name;
        Image = image;
        Time = time;
        Description = description;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getImage() {
        return Image;
    }

    public void setImage(int image) {
        Image = image;
    }

    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }
}
