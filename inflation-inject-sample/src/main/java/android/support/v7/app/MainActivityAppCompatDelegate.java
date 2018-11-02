package android.support.v7.app;

import com.squareup.inject.inflation.InflationInjectFactory;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivityAppCompatDelegate extends AppCompatDelegate implements
                                                                     LayoutInflater.Factory2 {

    @NonNull
    private final Context context;

    @NonNull
    private final AppCompatDelegate originalDelegate;

    @NonNull
    private final InflationInjectFactory inflationInjectionFactory;

    public MainActivityAppCompatDelegate(
            @NonNull Activity activity,
            @NonNull InflationInjectFactory factory) {
        this.context = activity;
        this.originalDelegate = new AppCompatDelegateImpl(activity, activity.getWindow(), null);
        this.inflationInjectionFactory = factory;
    }

    @Nullable
    @Override
    public ActionBar getSupportActionBar() {
        return originalDelegate.getSupportActionBar();
    }

    @Override
    public void setSupportActionBar(@Nullable final Toolbar toolbar) {
        originalDelegate.setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return originalDelegate.getMenuInflater();
    }

    @Override
    public void onCreate(final Bundle bundle) {
        originalDelegate.onCreate(bundle);
    }

    @Override
    public void onPostCreate(final Bundle bundle) {
        originalDelegate.onPostCreate(bundle);
    }

    @Override
    public void onConfigurationChanged(final Configuration configuration) {
        originalDelegate.onConfigurationChanged(configuration);
    }

    @Override
    public void onStart() {
        originalDelegate.onStart();
    }

    @Override
    public void onStop() {
        originalDelegate.onStop();
    }

    @Override
    public void onPostResume() {
        originalDelegate.onPostResume();
    }

    @Nullable
    @Override
    public <T extends View> T findViewById(final int i) {
        return originalDelegate.findViewById(i);
    }

    @Override
    public void setContentView(final View view) {
        originalDelegate.setContentView(view);
    }

    @Override
    public void setContentView(final int i) {
        originalDelegate.setContentView(i);
    }

    @Override
    public void setContentView(final View view, final ViewGroup.LayoutParams layoutParams) {
        originalDelegate.setContentView(view, layoutParams);
    }

    @Override
    public void addContentView(final View view, final ViewGroup.LayoutParams layoutParams) {
        originalDelegate.addContentView(view, layoutParams);
    }

    @Override
    public void setTitle(@Nullable final CharSequence charSequence) {
        originalDelegate.setTitle(charSequence);
    }

    @Override
    public void invalidateOptionsMenu() {
        originalDelegate.invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        originalDelegate.onDestroy();
    }

    @Nullable
    @Override
    public ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return originalDelegate.getDrawerToggleDelegate();
    }

    @Override
    public boolean requestWindowFeature(final int i) {
        return originalDelegate.requestWindowFeature(i);
    }

    @Override
    public boolean hasWindowFeature(final int i) {
        return originalDelegate.hasWindowFeature(i);
    }

    @Nullable
    @Override
    public ActionMode startSupportActionMode(@NonNull final ActionMode.Callback callback) {
        return originalDelegate.startSupportActionMode(callback);
    }

    @Override
    public void installViewFactory() {
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.setFactory2(this);
    }

    @Override
    public View createView(@Nullable final View view, final String s,
                           @NonNull final Context context,
                           @NonNull final AttributeSet attributeSet) {
        return originalDelegate.createView(view, s, context, attributeSet);
    }

    @Override
    public void setHandleNativeActionModesEnabled(final boolean b) {
        originalDelegate.setHandleNativeActionModesEnabled(b);
    }

    @Override
    public boolean isHandleNativeActionModesEnabled() {
        return originalDelegate.isHandleNativeActionModesEnabled();
    }

    @Override
    public void onSaveInstanceState(final Bundle bundle) {
        originalDelegate.onSaveInstanceState(bundle);
    }

    @Override
    public boolean applyDayNight() {
        return originalDelegate.applyDayNight();
    }

    @Override
    public void setLocalNightMode(final int i) {
        originalDelegate.setLocalNightMode(i);
    }

    @Override
    public View onCreateView(View view, final String s, final Context context,
                             final AttributeSet attributeSet) {
        final View delegateView = this.originalDelegate.createView(view, s, context, attributeSet);
        if (delegateView == null) {
            return inflationInjectionFactory.onCreateView(view, s, context, attributeSet);
        }
        return delegateView;
    }

    @Override
    public View onCreateView(final String s, final Context context,
                             final AttributeSet attributeSet) {
        final View delegateView = this.originalDelegate.createView(null, s, context, attributeSet);
        if (delegateView == null) {
            return inflationInjectionFactory.onCreateView(null, s, context, attributeSet);
        }
        return delegateView;
    }


}
