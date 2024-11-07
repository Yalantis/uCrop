# uCrop - Image Cropping Library for Android

#### This project aims to provide an ultimate and flexible image cropping experience. Made in [Yalantis](https://yalantis.com/?utm_source=github)

#### [How We Created uCrop](https://yalantis.com/blog/how-we-created-ucrop-our-own-image-cropping-library-for-android/)
#### Check this [project on Dribbble](https://dribbble.com/shots/2484752-uCrop-Image-Cropping-Library)

<img src="preview.gif" width="800" height="600">

# Usage

*For a working implementation, please have a look at the Sample Project - sample*

<a href="https://play.google.com/store/apps/details?id=com.yalantis.ucrop.sample&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-AC-global-none-all-co-pr-py-PartBadges-Oct1515-1"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="185" height="70"/></a>

1. Include the library as a local library project.

	```
	allprojects {
	   repositories {
	      jcenter()
	      maven { url "https://jitpack.io" }
	   }
	}
	```

    ``` implementation 'com.github.yalantis:ucrop:2.2.8' ``` - lightweight general solution

    ``` implementation 'com.github.yalantis:ucrop:2.2.8-native' ``` - get power of the native code to preserve image quality (+ about 1.5 MB to an apk size)

2. Add UCropActivity into your AndroidManifest.xml

    ```
    <activity
        android:name="com.yalantis.ucrop.UCropActivity"
        android:screenOrientation="portrait"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar"/>
    ```

3. The uCrop configuration is created using the builder pattern.

   ```java
   UCrop.of(sourceUri, destinationUri)
       .withAspectRatio(16, 9)
       .withMaxResultSize(maxWidth, maxHeight)
       .start(context);
   ```

4. Override `onActivityResult` method and handle uCrop result.

    ```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        }
    }
    ```

5. You may want to add this to your PROGUARD config:

    ```
    -dontwarn com.yalantis.ucrop**
    -keep class com.yalantis.ucrop** { *; }
    -keep interface com.yalantis.ucrop** { *; }
    ```

# Customization

If you want to let your users choose crop ratio dynamically, just do not call `withAspectRatio(x, y)`.

uCrop builder class has method `withOptions(UCrop.Options options)` which extends library configurations.

Currently, you can change:

   * image compression format (e.g. PNG, JPEG), compression
   * image compression quality [0 - 100]. PNG which is lossless, will ignore the quality setting.
   * whether all gestures are enabled simultaneously
   * maximum size for Bitmap that is decoded from source Uri and used within crop view. If you want to override the default behaviour.
   * toggle whether to show crop frame/guidelines
   * setup color/width/count of crop frame/rows/columns
   * choose whether you want rectangle or oval(`options.setCircleDimmedLayer(true)`) crop area
   * the UI colors (Toolbar, StatusBar, active widget state)
   * and more...

# Compatibility

  * Library - Android ICS 4.0+ (API 14) (Android GINGERBREAD 2.3+ (API 10) for versions <= 1.3.2)
  * Sample - Android ICS 4.0+ (API 14)
  * CPU - armeabi armeabi-v7a x86 x86_64 arm64-v8a (for versions >= 2.1.2)

# Changelog

### Version: 2.2.9

*   Update compileSdk and targetSdk versions up to 33
*   Fixed [#867](https://github.com/Yalantis/uCrop/issues/867)
*   Fixed [#873](https://github.com/Yalantis/uCrop/issues/873)
*   And other improvements

### Version: 2.2.8

*   Merged pending pull requests with improvements and bugfixes
*   Update compileSdk and targetSdk versions up to 31
*   Add localizations
*   Fixed [#609](https://github.com/Yalantis/uCrop/issues/609)
*   Fixed [#794](https://github.com/Yalantis/uCrop/issues/794)


### Version: 2.2.5

*   Fixed [#584](https://github.com/Yalantis/uCrop/issues/584)
*   Fixed [#598](https://github.com/Yalantis/uCrop/issues/598)
*   Fixed [#543](https://github.com/Yalantis/uCrop/issues/543)
*   Fixed [#602](https://github.com/Yalantis/uCrop/issues/602)
*   And other improvements

### Version: 2.2.4

  * **AndroidX migration**
  * Redesign
  * Several fixes including [#550](https://github.com/Yalantis/uCrop/issues/550)

### Version: 2.2.3

  * Several fixes including [#445](https://github.com/Yalantis/uCrop/issues/445), [#465](https://github.com/Yalantis/uCrop/issues/465) and more!
  * Material design support
  * uCrop fragment as child fragment
  * Added the Italian language

### Version: 2.2.2

* uCrop fragment added
* bugfix

### Version: 2.2.1

  * Fix including [#285](https://github.com/Yalantis/uCrop/issues/285)

### Version: 2.2

  * Several fixes including [#121](https://github.com/Yalantis/uCrop/issues/121), [#173](https://github.com/Yalantis/uCrop/issues/173), [#184](https://github.com/Yalantis/uCrop/issues/184) and more!
  * New APIs introduced [#149](https://github.com/Yalantis/uCrop/issues/149), [#186](https://github.com/Yalantis/uCrop/issues/186) and [#156](https://github.com/Yalantis/uCrop/issues/156)

### Version: 2.1

  * Fixes issue with EXIF data (images taken on front camera with Samsung devices mostly) [#130](https://github.com/Yalantis/uCrop/issues/130) [#111](https://github.com/Yalantis/uCrop/issues/111)
  * Added API to set custom set of aspect ratio options for the user. [#131](https://github.com/Yalantis/uCrop/issues/131)
  * Added API to set all configs via UCrop.Options class. [#126](https://github.com/Yalantis/uCrop/issues/126)
  * Added ABI x86_64 support. [#105](https://github.com/Yalantis/uCrop/issues/105)

### Version: 2.0

  * Native image crop (able to crop high-resolution images, e.g. 16MP & 32MP images on Nexus 5X).
  * WebP compression format is not supported at the moment (choose JPEG or PNG).
  * Now library copies EXIF data to cropped image (size and orientation are updated).

### Version: 1.5

  * Introduced "Freestyle" crop (you can resize crop rectangle by dragging it corners) [#32](https://github.com/Yalantis/uCrop/issues/32)
  * Now image & crop view paddings are not associated [#68](https://github.com/Yalantis/uCrop/issues/68)
  * Updated API

### Version: 1.4

  * Introduced HTTP(s) Uri support!
  * Image is cropped in a background thread.
  * Showing loader while Bitmap is processed (both loading and cropping).
  * Several bug fixes.
  * Couple new things to configure.
  * Updated minSdkVersion to Android ICS 4.0 (no reason to support couple percents of old phones).

### Version: 1.3

  * Image is loaded in a background thread. Better error-handling for image decoding.
  * Improved EXIF data support (rotation and mirror).
  * Small UI updates.
  * Couple new things to configure.

  * Sample updated with the possibility to choose custom aspect ratio.

### Version: 1.2

  * Updated core logic so an image corrects its position smoothly and obviously.

### Version: 1.1

  * UCrop builder was updated and now UCrop.Options class has even more values to setup.

### Version: 1.0

  * Initial Build

### Let us know!

We’d be really happy if you sent us links to your projects where you use our component. Just send an email to github@yalantis.com And do let us know if you have any questions or suggestion regarding the library.

#### Apps using uCrop

- [Thirty](https://play.google.com/store/apps/details?id=com.twominds.thirty).
- [Light Smart HD](https://play.google.com/store/apps/details?id=com.SmartCamera.simple).
- [BCReader](https://play.google.com/store/apps/details?id=com.iac.bcreader).
- [Xprezia: Share Your Passion](https://play.google.com/store/apps/details?id=com.xprezzia.cnj).

## License

    Copyright 2017, Yalantis

    Software doesn't collect, store or transfer data to Yalantis or third parties.
    Emplacement of this Software is carried out locally at device.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
