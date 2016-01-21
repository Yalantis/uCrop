package com.yalantis.ucrop;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yalantis.ucrop.util.BitmapLoadUtils;
import com.yalantis.ucrop.view.CropImageView;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.TransformImageView;
import com.yalantis.ucrop.view.widget.AspectRatioTextView;
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class UCropActivity extends AppCompatActivity {

    public static final int DEFAULT_MAX_BITMAP_SIZE = 0;
    public static final int DEFAULT_COMPRESS_QUALITY = 90;
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

    private static final String TAG = "UCropActivity";

    private static final int SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000;
    private static final int ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42;

    private GestureCropImageView mGestureCropImageView;
    private ViewGroup mWrapperStateAspectRatio, mWrapperStateRotate, mWrapperStateScale;
    private ViewGroup mLayoutAspectRatio, mLayoutRotate, mLayoutScale;
    private List<ViewGroup> mCropAspectRatioViews = new ArrayList<>();
    private TextView mTextViewRotateAngle, mTextViewScalePercent;

    private Uri mOutputUri;

    private int mMaxBitmapSize = DEFAULT_MAX_BITMAP_SIZE;
    private Bitmap.CompressFormat mCompressFormat = DEFAULT_COMPRESS_FORMAT;
    private int mCompressQuality = DEFAULT_COMPRESS_QUALITY;
    private boolean mGesturesAlwaysEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ucrop_activity_photobox);

        setupViews();
        setImageData();
        setInitialState();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.ucrop_menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_next) {
            cropAndSaveImage();
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGestureCropImageView != null) {
            mGestureCropImageView.cancelAllAnimations();
        }
    }

    private void setImageData() {
        final Intent intent = getIntent();

        Uri inputUri = intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI);
        mOutputUri = intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI);
        processOptions(intent);

        if (inputUri != null && mOutputUri != null) {
            try {
                mGestureCropImageView.setMaxBitmapSize(mMaxBitmapSize);
                mGestureCropImageView.setImageUri(inputUri);
            } catch (Exception e) {
                setResultException(e);
                finish();
            }
        } else {
            setResultException(new NullPointerException(getString(R.string.ucrop_error_input_data_is_absent)));
            finish();
        }

        if (intent.getBooleanExtra(UCrop.EXTRA_ASPECT_RATIO_SET, false)) {
            mWrapperStateAspectRatio.setVisibility(View.GONE);

            int aspectRatioX = intent.getIntExtra(UCrop.EXTRA_ASPECT_RATIO_X, 0);
            int aspectRatioY = intent.getIntExtra(UCrop.EXTRA_ASPECT_RATIO_Y, 0);

            if (aspectRatioX > 0 && aspectRatioY > 0) {
                mGestureCropImageView.setTargetAspectRatio(aspectRatioX / (float) aspectRatioY);
            } else {
                mGestureCropImageView.setTargetAspectRatio(CropImageView.SOURCE_IMAGE_ASPECT_RATIO);
            }
        }

        if (intent.getBooleanExtra(UCrop.EXTRA_MAX_SIZE_SET, false)) {
            int maxSizeX = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_X, 0);
            int maxSizeY = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_Y, 0);

            if (maxSizeX > 0 && maxSizeY > 0) {
                mGestureCropImageView.setMaxResultImageSizeX(maxSizeX);
                mGestureCropImageView.setMaxResultImageSizeY(maxSizeY);
            } else {
                Log.w(TAG, "EXTRA_MAX_SIZE_X and EXTRA_MAX_SIZE_Y must be greater than 0");
            }
        }
    }

    private void processOptions(@NonNull Intent intent) {
        UCrop.Options options = intent.getParcelableExtra(UCrop.EXTRA_OPTIONS);
        if (options != null) {
            mMaxBitmapSize = options.getMaxBitmapSize();

            String compressionFormatName = options.getCompressionFormatName();
            Bitmap.CompressFormat compressFormat = null;
            if (!TextUtils.isEmpty(compressionFormatName)) {
                compressFormat = Bitmap.CompressFormat.valueOf(compressionFormatName);
            }
            mCompressFormat = (compressFormat == null) ? DEFAULT_COMPRESS_FORMAT : compressFormat;

            mCompressQuality = options.getCompressionQuality();
            mGesturesAlwaysEnabled = options.isGesturesAlwaysEnabled();
        }
    }

    private void setupViews() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ucrop_ic_cross);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        setStatusBarColor(getResources().getColor(R.color.ucrop_color_statusbar));

        mGestureCropImageView = (GestureCropImageView) findViewById(R.id.image_view_crop);
        mGestureCropImageView.setTransformImageListener(new TransformImageView.TransformImageListener() {
            @Override
            public void onRotate(float currentAngle) {
                setAngleText(currentAngle);
            }

            @Override
            public void onScale(float currentScale) {
                setScaleText(currentScale);
            }
        });

        mWrapperStateAspectRatio = (ViewGroup) findViewById(R.id.state_aspect_ratio);
        mWrapperStateAspectRatio.setOnClickListener(mStateClickListener);
        mWrapperStateRotate = (ViewGroup) findViewById(R.id.state_rotate);
        mWrapperStateRotate.setOnClickListener(mStateClickListener);
        mWrapperStateScale = (ViewGroup) findViewById(R.id.state_scale);
        mWrapperStateScale.setOnClickListener(mStateClickListener);

        mLayoutAspectRatio = (ViewGroup) findViewById(R.id.layout_aspect_ratio);
        mLayoutRotate = (ViewGroup) findViewById(R.id.layout_rotate_wheel);
        mLayoutScale = (ViewGroup) findViewById(R.id.layout_scale_wheel);

        setupAspectRatioWidget();
        setupRotateWidget();
        setupScaleWidget();
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getWindow() != null) {
                getWindow().setStatusBarColor(color);
            }
        }
    }

    private void setupAspectRatioWidget() {
        mCropAspectRatioViews.add((ViewGroup) findViewById(R.id.crop_aspect_ratio_1_1));
        mCropAspectRatioViews.add((ViewGroup) findViewById(R.id.crop_aspect_ratio_3_4));
        mCropAspectRatioViews.add((ViewGroup) findViewById(R.id.crop_aspect_ratio_original));
        mCropAspectRatioViews.add((ViewGroup) findViewById(R.id.crop_aspect_ratio_3_2));
        mCropAspectRatioViews.add((ViewGroup) findViewById(R.id.crop_aspect_ratio_16_9));
        mCropAspectRatioViews.get(2).setSelected(true);

        for (ViewGroup cropAspectRatioView : mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mGestureCropImageView.setTargetAspectRatio(
                            ((AspectRatioTextView) ((ViewGroup) v).getChildAt(0)).getAspectRatio(v.isSelected()));
                    mGestureCropImageView.setImageToWrapCropBounds();
                    if (!v.isSelected()) {
                        for (ViewGroup cropAspectRatioView : mCropAspectRatioViews) {
                            cropAspectRatioView.setSelected(cropAspectRatioView == v);
                        }
                    }
                }
            });
        }
    }

    private void setupRotateWidget() {
        mTextViewRotateAngle = ((TextView) findViewById(R.id.text_view_rotate));
        ((HorizontalProgressWheelView) findViewById(R.id.rotate_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });


        findViewById(R.id.wrapper_reset_rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRotation();
            }
        });
        findViewById(R.id.wrapper_rotate_by_angle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateByAngle(90);
            }
        });
    }

    private void setupScaleWidget() {
        mTextViewScalePercent = ((TextView) findViewById(R.id.text_view_scale));
        ((HorizontalProgressWheelView) findViewById(R.id.scale_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        if (delta > 0) {
                            mGestureCropImageView.zoomInImage(mGestureCropImageView.getCurrentScale()
                                    + delta * ((mGestureCropImageView.getMaxScale() - mGestureCropImageView.getMinScale()) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT));
                        } else {
                            mGestureCropImageView.zoomOutImage(mGestureCropImageView.getCurrentScale()
                                    + delta * ((mGestureCropImageView.getMaxScale() - mGestureCropImageView.getMinScale()) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT));
                        }
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });
    }

    private void setAngleText(float angle) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle.setText(String.format("%.1f°", angle));
        }
    }

    private void setScaleText(float scale) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent.setText(String.format("%d%%", (int) (scale * 100)));
        }
    }

    private void resetRotation() {
        mGestureCropImageView.postRotate(-mGestureCropImageView.getCurrentAngle());
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private void rotateByAngle(int angle) {
        mGestureCropImageView.postRotate(angle);
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private final View.OnClickListener mStateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!v.isSelected()) {
                setWidgetState(v.getId());
            }
        }
    };

    private void setInitialState() {
        setWidgetState(R.id.state_scale);
    }

    private void setWidgetState(@IdRes int stateViewId) {
        mWrapperStateAspectRatio.setSelected(stateViewId == R.id.state_aspect_ratio);
        mWrapperStateRotate.setSelected(stateViewId == R.id.state_rotate);
        mWrapperStateScale.setSelected(stateViewId == R.id.state_scale);

        mLayoutAspectRatio.setVisibility(stateViewId == R.id.state_aspect_ratio ? View.VISIBLE : View.GONE);
        mLayoutRotate.setVisibility(stateViewId == R.id.state_rotate ? View.VISIBLE : View.GONE);
        mLayoutScale.setVisibility(stateViewId == R.id.state_scale ? View.VISIBLE : View.GONE);

        mGestureCropImageView.setRotateEnabled(mGesturesAlwaysEnabled || stateViewId != R.id.state_scale);
        mGestureCropImageView.setScaleEnabled(mGesturesAlwaysEnabled || stateViewId != R.id.state_rotate);
    }

    private void cropAndSaveImage() {
        OutputStream outputStream = null;
        try {
            final Bitmap croppedBitmap = mGestureCropImageView.cropImage();
            if (croppedBitmap != null) {
                outputStream = getContentResolver().openOutputStream(mOutputUri);
                croppedBitmap.compress(mCompressFormat, mCompressQuality, outputStream);
                croppedBitmap.recycle();

                setResultUri(mOutputUri);
                finish();
            }
        } catch (Exception e) {
            setResultException(e);
            finish();
        } finally {
            BitmapLoadUtils.close(outputStream);
        }
    }

    private void setResultUri(Uri uri) {
        setResult(RESULT_OK, new Intent().putExtra(UCrop.EXTRA_OUTPUT_URI, uri));
    }

    private void setResultException(Throwable throwable) {
        setResult(UCrop.RESULT_ERROR, new Intent().putExtra(UCrop.EXTRA_ERROR, throwable));
    }

}
