package com.yalantis.ucrop.sample;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.UCropFragment;
import com.yalantis.ucrop.UCropFragmentCallback;

import java.io.File;
import java.util.Locale;
import java.util.Random;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class SampleActivity extends BaseActivity implements UCropFragmentCallback {

    private static final String TAG = "SampleActivity";

    private static final int REQUEST_SELECT_PICTURE = 0x01;
    private static final int REQUEST_SELECT_PICTURE_FOR_FRAGMENT = 0x02;
    private static final int DESTINATION_IMAGE_FILE_REQUEST_CODE = 0x03;
    private static final String SAMPLE_CROPPED_IMAGE_NAME = "SampleCropImage";

    private RadioGroup mRadioGroupAspectRatio, mRadioGroupCompressionSettings, mRadioGroupChooseDestination;
    private EditText mEditTextMaxWidth, mEditTextMaxHeight;
    private EditText mEditTextRatioX, mEditTextRatioY;
    private CheckBox mCheckBoxMaxSize;
    private SeekBar mSeekBarQuality;
    private TextView mTextViewQuality;
    private CheckBox mCheckBoxHideBottomControls;
    private CheckBox mCheckBoxFreeStyleCrop;
    private Toolbar toolbar;
    private ScrollView settingsView;
    private int requestMode = BuildConfig.RequestMode;

    private UCropFragment fragment;
    private boolean mShowLoader;

    private String mToolbarTitle;
    @DrawableRes
    private int mToolbarCancelDrawable;
    @DrawableRes
    private int mToolbarCropDrawable;
    // Enables dynamic coloring
    private int mToolbarColor;
    private int mStatusBarColor;
    private int mToolbarWidgetColor;

    private TextView mFileDestinationTextView;
    private CheckBox mCheckBoxUseDocumentProvider;
    private CheckBox mCheckBoxUseFileProvider;
    private Uri destinationUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        setupUI();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == requestMode) {
                final Uri selectedUri = data.getData();
                if (selectedUri != null) {
                    startCrop(selectedUri);
                } else {
                    Toast.makeText(SampleActivity.this, R.string.toast_cannot_retrieve_selected_image, Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                handleCropResult(data);
            } else if (requestCode == DESTINATION_IMAGE_FILE_REQUEST_CODE) {
                destinationUri = data.getData();
                mFileDestinationTextView.setText(destinationUri.toString());
            }
        }
        if (resultCode == UCrop.RESULT_ERROR) {
            handleCropError(data);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery();
                }
                break;
            case REQUEST_STORAGE_WRITE_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showChooseFileDestinationAlertDialog();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private TextWatcher mAspectRatioTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mRadioGroupAspectRatio.clearCheck();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    private TextWatcher mMaxSizeTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s != null && !s.toString().trim().isEmpty()) {
                if (Integer.valueOf(s.toString()) < UCrop.MIN_SIZE) {
                    Toast.makeText(SampleActivity.this, String.format(getString(R.string.format_max_cropped_image_size), UCrop.MIN_SIZE), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private abstract class OcCropButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRadioGroupChooseDestination.getCheckedRadioButtonId() == R.id.radio_choose_destination && destinationUri == null) {
                Toast.makeText(SampleActivity.this,
                               "Please, select a destination file or set Tmp Files destination option",
                               Toast.LENGTH_LONG).show();
            } else {
                onValidatedClick(v);
            }
        }

        abstract void onValidatedClick(View v);
    }

    private class ButtonFileDestinationClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String destinationFileName = SAMPLE_CROPPED_IMAGE_NAME;
            String mimeType = "image/jpeg";
            switch (mRadioGroupCompressionSettings.getCheckedRadioButtonId()) {
                case R.id.radio_png:
                    mimeType = "image/png";
                    destinationFileName += ".png";
                    break;
                case R.id.radio_jpeg:
                    mimeType = "image/jpeg";
                    destinationFileName += ".jpg";
                    break;
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                if (mCheckBoxUseDocumentProvider.isChecked()) {

                    Intent createDocumentIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    createDocumentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    createDocumentIntent.setType(mimeType);
                    createDocumentIntent.putExtra(Intent.EXTRA_TITLE, destinationFileName);

                    if (createDocumentIntent.resolveActivity(getPackageManager()) != null) {

                        startActivityForResult(createDocumentIntent, DESTINATION_IMAGE_FILE_REQUEST_CODE);
                    } else {
                        Toast.makeText(SampleActivity.this,
                                       R.string.no_file_chooser_error,
                                       Toast.LENGTH_LONG).show();
                    }
                } else if(ActivityCompat.checkSelfPermission(SampleActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                      getString(R.string.permission_write_storage_rationale),
                                      REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
                } else {
                    showChooseFileDestinationAlertDialog();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && ActivityCompat.checkSelfPermission(SampleActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                  getString(R.string.permission_write_storage_rationale),
                                  REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
            } else {
                showChooseFileDestinationAlertDialog();
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setupUI() {
        findViewById(R.id.button_crop).setOnClickListener(new OcCropButtonClickListener() {
            @Override
            public void onValidatedClick(View v) {
                pickFromGallery();
            }
        });
        findViewById(R.id.button_random_image).setOnClickListener(new OcCropButtonClickListener() {
            @Override
            public void onValidatedClick(View v) {
                Random random = new Random();
                int minSizePixels = 800;
                int maxSizePixels = 2400;
                Uri uri = Uri.parse(String.format(Locale.getDefault(), "https://unsplash.it/%d/%d/?random",
                        minSizePixels + random.nextInt(maxSizePixels - minSizePixels),
                        minSizePixels + random.nextInt(maxSizePixels - minSizePixels)));

                startCrop(uri);
            }
        });
        mFileDestinationTextView = findViewById(R.id.file_destination_label);
        mCheckBoxUseDocumentProvider = findViewById(R.id.checkbox_use_document_provider);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mCheckBoxUseDocumentProvider.setVisibility(View.VISIBLE);
        }
        mCheckBoxUseFileProvider = findViewById(R.id.checkbox_use_file_provider);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mCheckBoxUseFileProvider.setVisibility(View.VISIBLE);
        }
        mCheckBoxUseDocumentProvider.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCheckBoxUseFileProvider.setEnabled(!isChecked);
            }
        });
        final Button buttonFileDestination = findViewById(R.id.button_file_destination);
        buttonFileDestination.setOnClickListener(new ButtonFileDestinationClickListener());
        mRadioGroupChooseDestination = findViewById(R.id.radio_group_choose_destination);
        mRadioGroupChooseDestination.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean isEnabledChoseDestination = R.id.radio_choose_destination == checkedId;
                buttonFileDestination.setEnabled(isEnabledChoseDestination);
                mCheckBoxUseDocumentProvider.setEnabled(isEnabledChoseDestination);

                if (isEnabledChoseDestination) {
                    mFileDestinationTextView.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        mCheckBoxUseFileProvider.setEnabled(true);
                    } else {
                        mCheckBoxUseFileProvider.setEnabled(!mCheckBoxUseDocumentProvider.isChecked());
                    }
                } else {
                    mCheckBoxUseFileProvider.setEnabled(false);
                    mFileDestinationTextView.setVisibility(View.GONE);
                    mFileDestinationTextView.setText("");
                    destinationUri = null;
                }
            }
        });
        mRadioGroupChooseDestination.check(R.id.radio_tmp_destination);
        settingsView = findViewById(R.id.settings);
        mRadioGroupAspectRatio = findViewById(R.id.radio_group_aspect_ratio);
        mRadioGroupCompressionSettings = findViewById(R.id.radio_group_compression_settings);
        mCheckBoxMaxSize = findViewById(R.id.checkbox_max_size);
        mEditTextRatioX = findViewById(R.id.edit_text_ratio_x);
        mEditTextRatioY = findViewById(R.id.edit_text_ratio_y);
        mEditTextMaxWidth = findViewById(R.id.edit_text_max_width);
        mEditTextMaxHeight = findViewById(R.id.edit_text_max_height);
        mSeekBarQuality = findViewById(R.id.seekbar_quality);
        mTextViewQuality = findViewById(R.id.text_view_quality);
        mCheckBoxHideBottomControls = findViewById(R.id.checkbox_hide_bottom_controls);
        mCheckBoxFreeStyleCrop = findViewById(R.id.checkbox_freestyle_crop);

        mRadioGroupAspectRatio.check(R.id.radio_dynamic);
        mEditTextRatioX.addTextChangedListener(mAspectRatioTextWatcher);
        mEditTextRatioY.addTextChangedListener(mAspectRatioTextWatcher);
        mRadioGroupCompressionSettings.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mSeekBarQuality.setEnabled(checkedId == R.id.radio_jpeg);
            }
        });
        mRadioGroupCompressionSettings.check(R.id.radio_jpeg);
        mSeekBarQuality.setProgress(UCropActivity.DEFAULT_COMPRESS_QUALITY);
        mTextViewQuality.setText(String.format(getString(R.string.format_quality_d), mSeekBarQuality.getProgress()));
        mSeekBarQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextViewQuality.setText(String.format(getString(R.string.format_quality_d), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mEditTextMaxHeight.addTextChangedListener(mMaxSizeTextWatcher);
        mEditTextMaxWidth.addTextChangedListener(mMaxSizeTextWatcher);
    }

    private void showChooseFileDestinationAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Destination File");

        builder.setView(R.layout.dialog_file_destination);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog thisDialog = ((AlertDialog) dialog);
                RadioGroup radioGroupDirectory = thisDialog.findViewById(R.id.radio_group_directory);
                EditText editTextFilename = thisDialog.findViewById(R.id.edit_text_file_name);

                String fileName = editTextFilename.getText().toString();
                if (!fileName.endsWith("jpg")) {
                    fileName = fileName.concat(".jpg");
                }

                File directory = null;

                switch (radioGroupDirectory.getCheckedRadioButtonId()) {
                    case R.id.radio_external_storage_dcim:
                        directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                        break;
                    case R.id.radio_external_storage_pictures:
                        directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        break;
                    case R.id.radio_app_external_storage_dcim:
                        directory = SampleActivity.this.getExternalFilesDir(Environment.DIRECTORY_DCIM);
                        break;
                    case R.id.radio_app_external_storage_pictures:
                        directory = SampleActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        break;
                }

                File file = new File(directory, fileName);
                if (mCheckBoxUseFileProvider.isChecked()) {
                    destinationUri = FileProvider.getUriForFile(SampleActivity.this,
                                                                getString(R.string.file_provider_authorities),
                                                                file);
                } else {
                    destinationUri = Uri.fromFile(file);
                }
                mFileDestinationTextView.setText(destinationUri.toString());
            }
        });
        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        // create and show the alert dialog
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        EditText editTextFilename = dialog.findViewById(R.id.edit_text_file_name);
        if(editTextFilename != null) {
            editTextFilename.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null && !s.toString().trim().isEmpty() && s.toString().trim().length() >= 3) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                }
            });
        }

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            RadioButton externalDcim = dialog.findViewById(R.id.radio_external_storage_dcim);
            RadioButton externalPictures = dialog.findViewById(R.id.radio_external_storage_pictures);
            RadioButton appDcim = dialog.findViewById(R.id.radio_app_external_storage_dcim);
            externalDcim.setEnabled(false);
            externalPictures.setEnabled(false);
            appDcim.setChecked(true);
        }
    }

    private void pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.permission_read_storage_rationale),
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                    .setType("image/*")
                    .addCategory(Intent.CATEGORY_OPENABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                String[] mimeTypes = {"image/jpeg", "image/png"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }

            startActivityForResult(Intent.createChooser(intent, getString(R.string.label_select_picture)), requestMode);
        }
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = SAMPLE_CROPPED_IMAGE_NAME;
        switch (mRadioGroupCompressionSettings.getCheckedRadioButtonId()) {
            case R.id.radio_png:
                destinationFileName += ".png";
                break;
            case R.id.radio_jpeg:
                destinationFileName += ".jpg";
                break;
        }

        if (destinationUri == null) {
            destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));
        }

        UCrop uCrop = UCrop.of(uri, destinationUri);

        uCrop = basisConfig(uCrop);
        uCrop = advancedConfig(uCrop);

        if (requestMode == REQUEST_SELECT_PICTURE_FOR_FRAGMENT) {       //if build variant = fragment
            setupFragment(uCrop);
        } else {                                                        // else start uCrop Activity
            uCrop.start(SampleActivity.this);
        }

    }

    /**
     * In most cases you need only to set crop aspect ration and max size for resulting image.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private UCrop basisConfig(@NonNull UCrop uCrop) {
        switch (mRadioGroupAspectRatio.getCheckedRadioButtonId()) {
            case R.id.radio_origin:
                uCrop = uCrop.useSourceImageAspectRatio();
                break;
            case R.id.radio_square:
                uCrop = uCrop.withAspectRatio(1, 1);
                break;
            case R.id.radio_dynamic:
                // do nothing
                break;
            default:
                try {
                    float ratioX = Float.valueOf(mEditTextRatioX.getText().toString().trim());
                    float ratioY = Float.valueOf(mEditTextRatioY.getText().toString().trim());
                    if (ratioX > 0 && ratioY > 0) {
                        uCrop = uCrop.withAspectRatio(ratioX, ratioY);
                    }
                } catch (NumberFormatException e) {
                    Log.i(TAG, String.format("Number please: %s", e.getMessage()));
                }
                break;
        }

        if (mCheckBoxMaxSize.isChecked()) {
            try {
                int maxWidth = Integer.valueOf(mEditTextMaxWidth.getText().toString().trim());
                int maxHeight = Integer.valueOf(mEditTextMaxHeight.getText().toString().trim());
                if (maxWidth > UCrop.MIN_SIZE && maxHeight > UCrop.MIN_SIZE) {
                    uCrop = uCrop.withMaxResultSize(maxWidth, maxHeight);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Number please", e);
            }
        }

        return uCrop;
    }

    /**
     * Sometimes you want to adjust more options, it's done via {@link com.yalantis.ucrop.UCrop.Options} class.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private UCrop advancedConfig(@NonNull UCrop uCrop) {
        UCrop.Options options = new UCrop.Options();

        switch (mRadioGroupCompressionSettings.getCheckedRadioButtonId()) {
            case R.id.radio_png:
                options.setCompressionFormat(Bitmap.CompressFormat.PNG);
                break;
            case R.id.radio_jpeg:
            default:
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                break;
        }
        options.setCompressionQuality(mSeekBarQuality.getProgress());

        options.setHideBottomControls(mCheckBoxHideBottomControls.isChecked());
        options.setFreeStyleCropEnabled(mCheckBoxFreeStyleCrop.isChecked());

        /*
        If you want to configure how gestures work for all UCropActivity tabs

        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        * */

        /*
        This sets max size for bitmap that will be decoded from source Uri.
        More size - more memory allocation, default implementation uses screen diagonal.

        options.setMaxBitmapSize(640);
        * */


       /*

        Tune everything (ﾉ◕ヮ◕)ﾉ*:･ﾟ✧

        options.setMaxScaleMultiplier(5);
        options.setImageToCropBoundsAnimDuration(666);
        options.setDimmedLayerColor(Color.CYAN);
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setCropGridStrokeWidth(20);
        options.setCropGridColor(Color.GREEN);
        options.setCropGridColumnCount(2);
        options.setCropGridRowCount(1);
        options.setToolbarCropDrawable(R.drawable.your_crop_icon);
        options.setToolbarCancelDrawable(R.drawable.your_cancel_icon);

        // Color palette
        options.setToolbarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));

        // Aspect ratio options
        options.setAspectRatioOptions(2,
            new AspectRatio("WOW", 1, 2),
            new AspectRatio("MUCH", 3, 4),
            new AspectRatio("RATIO", CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO),
            new AspectRatio("SO", 16, 9),
            new AspectRatio("ASPECT", 1, 1));
        options.withAspectRatio(CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO);
        options.useSourceImageAspectRatio();

       */

        return uCrop.withOptions(options);
    }

    private void handleCropResult(@NonNull Intent result) {
        final Uri resultUri = UCrop.getOutput(result);
        if (resultUri != null) {
            ResultActivity.startWithUri(SampleActivity.this, resultUri);
        } else {
            Toast.makeText(SampleActivity.this, R.string.toast_cannot_retrieve_cropped_image, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void handleCropError(@NonNull Intent result) {
        final Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            Log.e(TAG, "handleCropError: ", cropError);
            Toast.makeText(SampleActivity.this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(SampleActivity.this, R.string.toast_unexpected_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void loadingProgress(boolean showLoader) {
        mShowLoader = showLoader;
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onCropFinish(UCropFragment.UCropResult result) {
        switch (result.mResultCode) {
            case RESULT_OK:
                handleCropResult(result.mResultData);
                break;
            case UCrop.RESULT_ERROR:
                handleCropError(result.mResultData);
                break;
        }
        removeFragmentFromScreen();
    }

    public void removeFragmentFromScreen() {
        getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
        toolbar.setVisibility(View.GONE);
        settingsView.setVisibility(View.VISIBLE);
    }

    public void setupFragment(UCrop uCrop) {
        fragment = uCrop.getFragment(uCrop.getIntent(this).getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, UCropFragment.TAG)
                .commitAllowingStateLoss();

        setupViews(uCrop.getIntent(this).getExtras());
    }

    public void setupViews(Bundle args) {
        settingsView.setVisibility(View.GONE);
        mStatusBarColor = args.getInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR, ContextCompat.getColor(this, R.color.ucrop_color_statusbar));
        mToolbarColor = args.getInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR, ContextCompat.getColor(this, R.color.ucrop_color_toolbar));
        mToolbarCancelDrawable = args.getInt(UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE, R.drawable.ucrop_ic_cross);
        mToolbarCropDrawable = args.getInt(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE, R.drawable.ucrop_ic_done);
        mToolbarWidgetColor = args.getInt(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget));
        mToolbarTitle = args.getString(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR);
        mToolbarTitle = mToolbarTitle != null ? mToolbarTitle : getResources().getString(R.string.ucrop_label_edit_photo);

        setupAppBar();
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private void setupAppBar() {
        setStatusBarColor(mStatusBarColor);

        toolbar = findViewById(R.id.toolbar);

        // Set all of the Toolbar coloring
        toolbar.setBackgroundColor(mToolbarColor);
        toolbar.setTitleTextColor(mToolbarWidgetColor);
        toolbar.setVisibility(View.VISIBLE);
        final TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setTextColor(mToolbarWidgetColor);
        toolbarTitle.setText(mToolbarTitle);

        // Color buttons inside the Toolbar
        Drawable stateButtonDrawable = ContextCompat.getDrawable(getBaseContext(), mToolbarCancelDrawable);
        if (stateButtonDrawable != null) {
            stateButtonDrawable.mutate();
            stateButtonDrawable.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
            toolbar.setNavigationIcon(stateButtonDrawable);
        }

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(color);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.ucrop_menu_activity, menu);

        // Change crop & loader menu icons color to match the rest of the UI colors

        MenuItem menuItemLoader = menu.findItem(R.id.menu_loader);
        Drawable menuItemLoaderIcon = menuItemLoader.getIcon();
        if (menuItemLoaderIcon != null) {
            try {
                menuItemLoaderIcon.mutate();
                menuItemLoaderIcon.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
                menuItemLoader.setIcon(menuItemLoaderIcon);
            } catch (IllegalStateException e) {
                Log.i(this.getClass().getName(), String.format("%s - %s", e.getMessage(), getString(R.string.ucrop_mutate_exception_hint)));
            }
            ((Animatable) menuItemLoader.getIcon()).start();
        }

        MenuItem menuItemCrop = menu.findItem(R.id.menu_crop);
        Drawable menuItemCropIcon = ContextCompat.getDrawable(this, mToolbarCropDrawable == 0 ? R.drawable.ucrop_ic_done : mToolbarCropDrawable);
        if (menuItemCropIcon != null) {
            menuItemCropIcon.mutate();
            menuItemCropIcon.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
            menuItemCrop.setIcon(menuItemCropIcon);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_crop).setVisible(!mShowLoader);
        menu.findItem(R.id.menu_loader).setVisible(mShowLoader);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_crop) {
            if (fragment != null && fragment.isAdded())
                fragment.cropAndSaveImage();
        } else if (item.getItemId() == android.R.id.home) {
            removeFragmentFromScreen();
        }
        return super.onOptionsItemSelected(item);
    }
}
