package com.ranshinban.ranshinban.utils;

public class errorWindow
{
    static public void raiseErrorWindow(String message)
    {
        popupWindow.raisePopupWindow(message,"ERROR");
    }
}
