package com.rocks.voodoo.library;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.RequiresPermission;

public final class ConnectionUtil {
    private Context context;

    public ConnectionUtil(Context context) {
        this.context = context;
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public boolean isOnline(){
        return isOnline(context);
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        return info != null && info.isConnectedOrConnecting();
    }

}
