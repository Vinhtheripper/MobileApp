package com.example.mpos.auth;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import com.example.mpos.constants.RoleConstants;

public final class PermissionHelper {
    private PermissionHelper() { }

    public static boolean isAdmin(Activity activity) {
        return RoleConstants.ADMIN.equals(new SessionManager(activity).getUser().role);
    }

    public static boolean isStaff(Activity activity) {
        return RoleConstants.STAFF.equals(new SessionManager(activity).getUser().role);
    }

    public static boolean canManageAdmin(Activity activity) {
        return isAdmin(activity);
    }

    public static boolean canManageCatalog(Activity activity) {
        return isAdmin(activity);
    }

    public static boolean canManageInventory(Activity activity) {
        return isAdmin(activity);
    }

    public static boolean canViewReports(Activity activity) {
        return isAdmin(activity);
    }

    public static boolean requireAdmin(Activity activity) {
        if (isAdmin(activity)) return true;
        deny(activity);
        return false;
    }

    public static void showIf(View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private static void deny(Activity activity) {
        Toast.makeText(activity, "Bạn không có quyền truy cập chức năng này", Toast.LENGTH_LONG).show();
        activity.finish();
    }
}
