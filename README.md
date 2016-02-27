# uCrop - Image Cropping Library for Android

#### This project aims to provide an ultimate and flexible image cropping experience. Made in [Yalantis] (https://yalantis.com/?utm_source=github)

#### [How We Created uCrop] (https://yalantis.com/blog/how-we-created-ucrop-our-own-image-cropping-library-for-android/)

<img src="https://d13yacurqjgara.cloudfront.net/users/221935/screenshots/2474295/animation.gif" alt="alt text" style="width:200;height:200">

# Usage

*For a working implementation, please have a look at the Sample Project - sample*

<a href="https://play.google.com/store/apps/details?id=com.yalantis.ucrop.sample&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-AC-global-none-all-co-pr-py-PartBadges-Oct1515-1"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge.png" width="185" height="60"/></a>

1. Include the library as local library project.

    ``` compile 'com.yalantis:ucrop:1.3.+' ```
    
2. Add UCropActivity into your AndroidManifest.xml

    ```
    <activity
        android:name="com.yalantis.ucrop.UCropActivity"
        android:screenOrientation="portrait"/>
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


# Customization

If you want to let your users choose crop ratio dynamically, just do not call `withAspectRatio(x, y)`.

uCrop builder class has method `withOptions(UCrop.Options options)` which extends library configurations.

Currently you can change:

   * image compression format (e.g. PNG, JPEG, WEBP), compression
   * image compression quality [0 - 100]. PNG which is lossless, will ignore the quality setting.
   * whether all gestures are enabled simultaneously
   * maximum size for Bitmap that is decoded from source Uri and used within crop view. If you want to override default behaviour.
   * toggle whether to show crop frame/guidelines
   * setup color/width/count of crop frame/rows/columns
   * choose whether you want rectangle or oval crop area
   * the UI colors (Toolbar, StatusBar, active widget state)
   * and more...
    
# Compatibility
  
  * Library - Android GINGERBREAD 2.3+
  * Sample - Android ICS 4.0+
  
# Changelog

### Version: 1.3

  * Image is loaded in background thread. Better error-handling for image decoding.
  * Improved EXIF data support (rotation and mirror).
  * Small UI updates.
  * Couple new things to configure
  
  * Sample updated with possibility to choose custom aspect ratio.

### Version: 1.2

  * Updated core logic so an image corrects its position smoothly and obviously.

### Version: 1.1

  * UCrop builder was updated and now UCrop.Options class has even more values to setup.

### Version: 1.0

  * Initial Build

### Let us know!

We’d be really happy if you sent us links to your projects where you use our component. Just send an email to github@yalantis.com And do let us know if you have any questions or suggestion regarding the library. 

## License

    Copyright 2016, Yalantis

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.