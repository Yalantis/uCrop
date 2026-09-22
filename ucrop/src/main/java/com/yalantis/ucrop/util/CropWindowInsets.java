package com.yalantis.ucrop.util;

import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Reserves only the part of the window's unsafe edges overlapping this crop page.
 * Padding changes the viewport, never the selectable source-image bounds.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CropWindowInsets {
    private CropWindowInsets() {
    }

    public static void install(@NonNull View root, @Nullable View toolbar) {
        new Handler(root, toolbar).install();
    }

    private static final class Handler implements View.OnAttachStateChangeListener,
            ViewTreeObserver.OnGlobalLayoutListener {
        private final View root;
        private final View toolbar;
        private final Insets base;
        private final Insets toolbarBase;
        private WindowInsetsCompat insets;
        private ViewTreeObserver observer;

        Handler(View root, View toolbar) {
            this.root = root;
            this.toolbar = toolbar;
            base = padding(root);
            toolbarBase = toolbar == null ? Insets.NONE : padding(toolbar);
        }

        void install() {
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
                insets = windowInsets;
                apply();
                return windowInsets;
            });
            root.addOnAttachStateChangeListener(this);
            if (ViewCompat.isAttachedToWindow(root)) {
                onViewAttachedToWindow(root);
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            observer = root.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(this);
            ViewCompat.requestApplyInsets(root);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            if (observer != null && observer.isAlive()) {
                observer.removeOnGlobalLayoutListener(this);
            }
            observer = null;
            insets = null;
        }

        @Override
        public void onGlobalLayout() {
            apply();
        }

        private void apply() {
            if (insets == null || !ViewCompat.isAttachedToWindow(root)
                    || root.getWidth() == 0 || root.getHeight() == 0) {
                return;
            }
            int types = WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.mandatorySystemGestures()
                    | WindowInsetsCompat.Type.displayCutout();
            Insets remaining = insets.getInsets(types);
            WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(root);
            // A parent may deliver reduced insets. Locate the original unsafe boundary
            // in the window, then cap our reservation by what that parent left unhandled.
            Insets full = windowInsets == null ? remaining
                    : Insets.max(remaining, windowInsets.getInsets(types));
            Rect window = windowBounds(root);
            int[] location = new int[2];
            root.getLocationOnScreen(location);
            int left = overlap(remaining.left, window.left + full.left - location[0]);
            int top = overlap(remaining.top, window.top + full.top - location[1]);
            int right = overlap(remaining.right,
                    location[0] + root.getWidth() - (window.right - full.right));
            int bottom = overlap(remaining.bottom,
                    location[1] + root.getHeight() - (window.bottom - full.bottom));

            setPadding(root, base.left + left, base.top + (toolbar == null ? top : 0),
                    base.right + right, base.bottom + bottom);
            if (toolbar != null) {
                // Root side padding already protects the toolbar. Its own top padding
                // keeps its background edge-to-edge without insetting the crop twice.
                setPadding(toolbar, toolbarBase.left,
                        toolbarBase.top + top,
                        toolbarBase.right, toolbarBase.bottom);
            }
        }
    }

    private static int overlap(int remaining, int overlap) {
        return Math.max(0, Math.min(remaining, overlap));
    }

    private static Insets padding(View view) {
        return Insets.of(view.getPaddingLeft(), view.getPaddingTop(),
                view.getPaddingRight(), view.getPaddingBottom());
    }

    private static void setPadding(View view, int left, int top, int right, int bottom) {
        // Physical edges, not start/end: a left navigation bar stays left in RTL.
        if (view.getPaddingLeft() != left || view.getPaddingTop() != top
                || view.getPaddingRight() != right || view.getPaddingBottom() != bottom) {
            view.setPadding(left, top, right, bottom);
        }
    }

    private static Rect windowBounds(View view) {
        if (Build.VERSION.SDK_INT >= 30) {
            return view.getContext().getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics().getBounds();
        }
        // Use the decor's window geometry, not the (possibly padded/embedded) crop root.
        View decor = view.getRootView();
        int[] screen = new int[2];
        int[] window = new int[2];
        decor.getLocationOnScreen(screen);
        decor.getLocationInWindow(window);
        int x = screen[0] - window[0];
        int y = screen[1] - window[1];
        return new Rect(x, y, x + decor.getWidth(), y + decor.getHeight());
    }
}
