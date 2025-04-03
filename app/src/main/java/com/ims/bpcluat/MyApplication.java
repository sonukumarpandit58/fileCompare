package com.ims.bpcluat;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class MyApplication extends Application {
    private boolean backButtonEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                final Window.Callback originalCallback = activity.getWindow().getCallback();
                activity.getWindow().setCallback(new Window.Callback() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                            return !isBackButtonEnabled();
                        }
                        return originalCallback.dispatchKeyEvent(event);
                    }

                    // Other callback methods that need to be forwarded to the original callback
                    @Override
                    public boolean dispatchKeyShortcutEvent(KeyEvent event) { return originalCallback.dispatchKeyShortcutEvent(event); }

                    @Override
                    public boolean dispatchTouchEvent(MotionEvent event) { return originalCallback.dispatchTouchEvent(event); }
                    @Override
                    public boolean dispatchTrackballEvent(MotionEvent event) { return originalCallback.dispatchTrackballEvent(event); }
                    @Override
                    public boolean dispatchGenericMotionEvent(MotionEvent event) { return originalCallback.dispatchGenericMotionEvent(event); }

                    @Override
                    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) { return originalCallback.dispatchPopulateAccessibilityEvent(event); }
                    @Override
                    public View onCreatePanelView(int featureId) { return originalCallback.onCreatePanelView(featureId); }

                    @Override
                    public boolean onCreatePanelMenu(int featureId, Menu menu) { return originalCallback.onCreatePanelMenu(featureId, menu); }
                    @Override
                    public boolean onPreparePanel(int featureId, View view, Menu menu) { return originalCallback.onPreparePanel(featureId, view, menu); }
                    @Override
                    public boolean onMenuOpened(int featureId, Menu menu) { return originalCallback.onMenuOpened(featureId, menu); }

                    @Override
                    public boolean onMenuItemSelected(int featureId, MenuItem item) { return originalCallback.onMenuItemSelected(featureId, item); }
                    @Override
                    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) { originalCallback.onWindowAttributesChanged(attrs); }
                    @Override
                    public void onContentChanged() { originalCallback.onContentChanged(); }
                    @Override
                    public void onWindowFocusChanged(boolean hasFocus) { originalCallback.onWindowFocusChanged(hasFocus); }
                    @Override
                    public void onAttachedToWindow() { originalCallback.onAttachedToWindow(); }
                    @Override
                    public void onDetachedFromWindow() { originalCallback.onDetachedFromWindow(); }
                    @Override
                    public void onPanelClosed(int featureId, Menu menu) { originalCallback.onPanelClosed(featureId, menu); }
                    @Override
                    public boolean onSearchRequested() { return originalCallback.onSearchRequested(); }

                    @Override
                    public boolean onSearchRequested(SearchEvent searchEvent) { return originalCallback.onSearchRequested(searchEvent); }
                    @Override
                    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) { return originalCallback.onWindowStartingActionMode(callback); }
                    @Override
                    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) { return originalCallback.onWindowStartingActionMode(callback, type); }
                    @Override
                    public void onActionModeStarted(ActionMode mode) { originalCallback.onActionModeStarted(mode); }
                    @Override
                    public void onActionModeFinished(ActionMode mode) { originalCallback.onActionModeFinished(mode); }
                });
            }

            @Override
            public void onActivityStarted(Activity activity) { }

            @Override
            public void onActivityResumed(Activity activity) { }

            @Override
            public void onActivityPaused(Activity activity) { }

            @Override
            public void onActivityStopped(Activity activity) { }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

            @Override
            public void onActivityDestroyed(Activity activity) { }
        });
    }

    public boolean isBackButtonEnabled() {
        return backButtonEnabled;
    }

    public void setBackButtonEnabled(boolean enabled) {
        backButtonEnabled = enabled;
    }
}