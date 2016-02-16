package com.yalantis.ucrop;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yalantis.ucrop.util.BitmapLoadUtils;
import com.yalantis.ucrop.view.CropImageView;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.TransformImageView;
import com.yalantis.ucrop.view.UCropView;
import com.yalantis.ucrop.view.widget.AspectRatioTextView;
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView;

import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class UCropActivity extends AppCompatActivity {

    public static final int DEFAULT_COMPRESS_QUALITY = 90;
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

    public static final int NONE = 0;
    public static final int SCALE = 1;
    public static final int ROTATE = 2;
    public static final int ALL = 3;

    @IntDef({NONE, SCALE, ROTATE, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GestureTypes {

    }

    private static final String TAG = "UCropActivity";

    private static final int TABS_COUNT = 3;
    private static final int SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000;
    private static final int ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42;

	// Enables dynamic coloring
	private int TOOLBAR_COLOR = -1;
	private int STATUS_BAR_COLOR = -1;
	private int ACTIVE_WIDGET_COLOR = -1;
	private int PROGRESS_WHEEL_LINE_COLOR = -1;
	private int TOOLBAR_TEXT_COLOR = -1;
	
    private GestureCropImageView mGestureCropImageView;
    private OverlayView mOverlayView;
    private ViewGroup mWrapperStateAspectRatio, mWrapperStateRotate, mWrapperStateScale;
    private ViewGroup mLayoutAspectRatio, mLayoutRotate, mLayoutScale;
    private List<ViewGroup> mCropAspectRatioViews = new ArrayList<>();
    private TextView mTextViewRotateAngle, mTextViewScalePercent;

    private Uri mOutputUri;

    private Bitmap.CompressFormat mCompressFormat = DEFAULT_COMPRESS_FORMAT;
    private int mCompressQuality = DEFAULT_COMPRESS_QUALITY;
    private int[] mAllowedGestures = new int[]{SCALE, ROTATE, ALL};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ucrop_activity_photobox);

		// Make sure that the colors are not empty (==-1) before proceeding
		STATUS_BAR_COLOR = getResources().getColor(R.color.ucrop_color_statusbar);
		TOOLBAR_COLOR = getResources().getColor(R.color.ucrop_color_toolbar);
		ACTIVE_WIDGET_COLOR = getResources().getColor(R.color.ucrop_color_widget_active);
		PROGRESS_WHEEL_LINE_COLOR = getResources().getColor(R.color.ucrop_color_progress_wheel_line);
		TOOLBAR_TEXT_COLOR = getResources().getColor(R.color.ucrop_color_title);
		
		// Then check if the intent contains the color data
		final Intent intent = getIntent();
        Bundle optionsBundle = intent.getBundleExtra(UCrop.EXTRA_OPTIONS);
		
		if(optionsBundle.containsKey(UCrop.Options.EXTRA_TOOL_BAR_COLOR)){
			TOOLBAR_COLOR = optionsBundle.getInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR);
		}
	
		if(optionsBundle.containsKey(UCrop.Options.EXTRA_STATUS_BAR_COLOR)){
			STATUS_BAR_COLOR = optionsBundle.getInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR);
		}
	
		if(optionsBundle.containsKey(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE)){
			ACTIVE_WIDGET_COLOR = optionsBundle.getInt(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE);
		}
	
		if(optionsBundle.containsKey(UCrop.Options.EXTRA_UCROP_COLOR_PROGRESS_WHEEL_LINE)){
			PROGRESS_WHEEL_LINE_COLOR = optionsBundle.getInt(UCrop.Options.EXTRA_UCROP_COLOR_PROGRESS_WHEEL_LINE);
		}
		
		if(optionsBundle.containsKey(UCrop.Options.EXTRA_UCROP_TITLE_COLOR_TOOLBAR)){
			TOOLBAR_TEXT_COLOR = optionsBundle.getInt(UCrop.Options.EXTRA_UCROP_TITLE_COLOR_TOOLBAR);
		}
	
        setupViews();
        setImageData();
        setInitialState();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.ucrop_menu_activity, menu);
		
		// Change the next menu icon color to match the rest of the UI colors
		MenuItem next = menu.findItem(R.id.menu_next);
		
		Drawable defaultIcon = next.getIcon();
		if(defaultIcon != null){
			defaultIcon.mutate();
			defaultIcon.setColorFilter(TOOLBAR_TEXT_COLOR, PorterDuff.Mode.SRC_ATOP);
			next.setIcon(defaultIcon);
		}
		
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

    /**
     * This method extracts all data from the incoming intent and setups views properly.
     */
    private void setImageData() {
        final Intent intent = getIntent();

        Uri inputUri = intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI);
        mOutputUri = intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI);
        processOptions(intent);

        if (inputUri != null && mOutputUri != null) {
            try {
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

    /**
     * This method extracts {@link com.yalantis.ucrop.UCrop.Options #optionsBundle} from incoming intent
     * and setups Activity, {@link OverlayView} and {@link CropImageView} properly.
     */
    @SuppressWarnings("deprecation")
    private void processOptions(@NonNull Intent intent) {
        Bundle optionsBundle = intent.getBundleExtra(UCrop.EXTRA_OPTIONS);
        if (optionsBundle != null) {
            // Bitmap compression options
            String compressionFormatName = optionsBundle.getString(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME);
            Bitmap.CompressFormat compressFormat = null;
            if (!TextUtils.isEmpty(compressionFormatName)) {
                compressFormat = Bitmap.CompressFormat.valueOf(compressionFormatName);
            }
            mCompressFormat = (compressFormat == null) ? DEFAULT_COMPRESS_FORMAT : compressFormat;

            mCompressQuality = optionsBundle.getInt(UCrop.Options.EXTRA_COMPRESSION_QUALITY, UCropActivity.DEFAULT_COMPRESS_QUALITY);

            // Gestures options
            int[] allowedGestures = optionsBundle.getIntArray(UCrop.Options.EXTRA_ALLOWED_GESTURES);
            if (allowedGestures != null && allowedGestures.length == TABS_COUNT) {
                mAllowedGestures = allowedGestures;
            }

            // Crop image view options
            mGestureCropImageView.setMaxBitmapSize(optionsBundle.getInt(UCrop.Options.EXTRA_MAX_BITMAP_SIZE, CropImageView.DEFAULT_MAX_BITMAP_SIZE));
            mGestureCropImageView.setMaxScaleMultiplier(optionsBundle.getFloat(UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER, CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER));
            mGestureCropImageView.setImageToWrapCropBoundsAnimDuration(optionsBundle.getInt(UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION, CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION));


            // Overlay view options
            mOverlayView.setDimmedColor(optionsBundle.getInt(UCrop.Options.EXTRA_DIMMED_LAYER_COLOR, getResources().getColor(R.color.ucrop_color_default_dimmed)));
            mOverlayView.setOvalDimmedLayer(optionsBundle.getBoolean(UCrop.Options.EXTRA_OVAL_DIMMED_LAYER, OverlayView.DEFAULT_OVAL_DIMMED_LAYER));

            mOverlayView.setShowCropFrame(optionsBundle.getBoolean(UCrop.Options.EXTRA_SHOW_CROP_FRAME, OverlayView.DEFAULT_SHOW_CROP_FRAME));
            mOverlayView.setCropFrameColor(optionsBundle.getInt(UCrop.Options.EXTRA_CROP_FRAME_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_frame)));
            mOverlayView.setCropFrameStrokeWidth(optionsBundle.getInt(UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width)));

            mOverlayView.setShowCropGrid(optionsBundle.getBoolean(UCrop.Options.EXTRA_SHOW_CROP_GRID, OverlayView.DEFAULT_SHOW_CROP_GRID));
            mOverlayView.setCropGridRowCount(optionsBundle.getInt(UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT, OverlayView.DEFAULT_CROP_GRID_ROW_COUNT));
            mOverlayView.setCropGridColumnCount(optionsBundle.getInt(UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT, OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT));
            mOverlayView.setCropGridColor(optionsBundle.getInt(UCrop.Options.EXTRA_CROP_GRID_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_grid)));
            mOverlayView.setCropGridStrokeWidth(optionsBundle.getInt(UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)));
        }
    }

    private void setupViews() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
				
		// Set all of the Toolbar coloring
		if(TOOLBAR_COLOR != -1){
			toolbar.setBackgroundColor(TOOLBAR_COLOR);
		}
		if(TOOLBAR_TEXT_COLOR != -1){
			toolbar.setTitleTextColor(TOOLBAR_TEXT_COLOR);
			toolbar.setSubtitleTextColor(TOOLBAR_TEXT_COLOR);
			
			((TextView)toolbar.findViewById(R.id.toolbar_title)).setTextColor(TOOLBAR_TEXT_COLOR);
			
			// Color all of the buttons inside the Toolbar
			
			StateListDrawable stateButtonSelector = new StateListDrawable();
			
			Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ucrop_ic_cross).mutate();
			stateButtonDrawable.setColorFilter(TOOLBAR_TEXT_COLOR, PorterDuff.Mode.SRC_ATOP);
			stateButtonSelector.addState(new int[]{android.R.attr.state_selected}, stateButtonDrawable);
			stateButtonSelector.addState(new int[0], stateButtonDrawable);
			
			toolbar.setNavigationIcon(stateButtonSelector);
			
		}else{
			toolbar.setNavigationIcon(R.drawable.ucrop_ic_cross);
		}
		
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
		
		if(STATUS_BAR_COLOR != -1){
			setStatusBarColor(STATUS_BAR_COLOR);
		}        

        UCropView uCropView = (UCropView) findViewById(R.id.ucrop);
        mGestureCropImageView = uCropView.getCropImageView();
        mOverlayView = uCropView.getOverlayView();

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
        setupStatesWrapper();
    }

    /**
     * use {@link ACTIVE_WIDGET_COLOR} for color filter
     */
    private void setupStatesWrapper() {
        ImageView stateScaleImageView = (ImageView) findViewById(R.id.image_view_state_scale);
        ImageView stateRotateImageView = (ImageView) findViewById(R.id.image_view_state_rotate);
        ImageView stateAspectRatioImageView = (ImageView) findViewById(R.id.image_view_state_aspect_ratio);

        StateListDrawable stateScaleSelector = new StateListDrawable();
        StateListDrawable stateRotateSelector = new StateListDrawable();
        StateListDrawable stateAspectRatioSelector = new StateListDrawable();

        Drawable stateScaleSelectedDrawable = ContextCompat.getDrawable(this, R.drawable.ucrop_ic_scale).mutate();
        stateScaleSelectedDrawable.setColorFilter(ACTIVE_WIDGET_COLOR, PorterDuff.Mode.SRC_ATOP);
        stateScaleSelector.addState(new int[]{android.R.attr.state_selected}, stateScaleSelectedDrawable);
        stateScaleSelector.addState(new int[0], ContextCompat.getDrawable(this, R.drawable.ucrop_ic_scale));

        Drawable stateRotateSelectedDrawable = ContextCompat.getDrawable(this, R.drawable.ucrop_ic_rotate).mutate();
        stateRotateSelectedDrawable.setColorFilter(ACTIVE_WIDGET_COLOR, PorterDuff.Mode.SRC_ATOP);
        stateRotateSelector.addState(new int[]{android.R.attr.state_selected}, stateRotateSelectedDrawable);
        stateRotateSelector.addState(new int[0], ContextCompat.getDrawable(this, R.drawable.ucrop_ic_rotate));

        Drawable stateAspectRatioSelectedDrawable = ContextCompat.getDrawable(this, R.drawable.ucrop_ic_crop).mutate();
        stateAspectRatioSelectedDrawable.setColorFilter(ACTIVE_WIDGET_COLOR, PorterDuff.Mode.SRC_ATOP);
        stateAspectRatioSelector.addState(new int[]{android.R.attr.state_selected}, stateAspectRatioSelectedDrawable);
        stateAspectRatioSelector.addState(new int[0], ContextCompat.getDrawable(this, R.drawable.ucrop_ic_crop));

        stateScaleImageView.setImageDrawable(stateScaleSelector);
        stateRotateImageView.setImageDrawable(stateRotateSelector);
        stateAspectRatioImageView.setImageDrawable(stateAspectRatioSelector);
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
		
		// Set the colors before the default item is selected
		((AspectRatioTextView)((ViewGroup)findViewById(R.id.crop_aspect_ratio_1_1)).getChildAt(0)).setColor(ACTIVE_WIDGET_COLOR);
		((AspectRatioTextView)((ViewGroup)findViewById(R.id.crop_aspect_ratio_3_4)).getChildAt(0)).setColor(ACTIVE_WIDGET_COLOR);
		((AspectRatioTextView)((ViewGroup)findViewById(R.id.crop_aspect_ratio_original)).getChildAt(0)).setColor(ACTIVE_WIDGET_COLOR);
		((AspectRatioTextView)((ViewGroup)findViewById(R.id.crop_aspect_ratio_3_2)).getChildAt(0)).setColor(ACTIVE_WIDGET_COLOR);
		((AspectRatioTextView)((ViewGroup)findViewById(R.id.crop_aspect_ratio_16_9)).getChildAt(0)).setColor(ACTIVE_WIDGET_COLOR);
		
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
				
		((HorizontalProgressWheelView) findViewById(R.id.rotate_scroll_wheel)).setLineColor(PROGRESS_WHEEL_LINE_COLOR);


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
		((HorizontalProgressWheelView) findViewById(R.id.scale_scroll_wheel)).setLineColor(PROGRESS_WHEEL_LINE_COLOR);
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

        if (stateViewId == R.id.state_scale) {
            setAllowedGestures(0);
        } else if (stateViewId == R.id.state_rotate) {
            setAllowedGestures(1);
        } else {
            setAllowedGestures(2);
        }
    }

    private void setAllowedGestures(int tab) {
        mGestureCropImageView.setScaleEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE);
        mGestureCropImageView.setRotateEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE);
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
            } else {
                setResultException(new NullPointerException("CropImageView.cropImage() returned null."));
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
