package com.apps.ing3ns.entregas.Services.GpsServices;

/**
 * Created by JuanDa on 27/08/2017.
 */

public class Constants {
    public interface ACTION {
        public static String START_FOREGROUND_SHARE = "com.ing3ns.foregroundservice.action.startforegroundshare";
        public static String STOP_FOREGROUND = "com.ing3ns.foregroundservice.action.stopforeground";
        public static String START_FOREGROUND= "com.ing3ns.foregroundservice.action.startforeground";
        public static int DELIVERY_PROCESS = 1001;
        public static int SEARCH_DELIVERY = 1002;
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE_GPS = 101;
    }
}
