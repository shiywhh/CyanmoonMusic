package com.magicalstory.music.dialog.bottomMenuDialog;

/**
 * @Classname: bottomDialogMenu
 * @Auther: Created by 奇谈君 on 2024/10/8.
 * @Description:
 */

public class bottomDialogMenu {
    String title;
    int icon = -1;
    boolean checked;
    private String extra="";

    public bottomDialogMenu(String title, int icon, boolean checked) {
        this.title = title;
        this.icon = icon;
        this.checked = checked;
    }

    public bottomDialogMenu(String title, String extra) {
        this.title = title;
        this.extra = extra;
    }
    public bottomDialogMenu(String title, boolean checked, String extra) {
        this.title = title;
        this.icon = icon;
        this.checked = checked;
        this.extra = extra;
    }

    public int getIcon() {
        return icon;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public bottomDialogMenu(String title) {
        this.title = title;
    }

    public bottomDialogMenu(String title, boolean checked) {
        this.title = title;
        this.checked = checked;
    }

    public bottomDialogMenu(String title, int icon) {
        this.title = title;
        this.icon = icon;
    }
}