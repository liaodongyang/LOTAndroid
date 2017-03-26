package com.openlib.photopicker.beans;

import java.io.Serializable;
import java.util.List;

/**
 * @Class: PhotoFolder
 * @Description: Image file class
 */
public class PhotoFolder implements Serializable {

    /* Folder name */
    private String name;
    /*  Folder path*/
    private String dirPath;
    /* Folder photo list */
    private List<Photo> photoList;
    /* choose folder? */
    private boolean isSelected;

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public List<Photo> getPhotoList() {
        return photoList;
    }

    public void setPhotoList(List<Photo> photoList) {
        this.photoList = photoList;
    }
}
