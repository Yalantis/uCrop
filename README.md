# uCrop - Image Cropping Library for Android

#### This project aims to provide an ultimate and flexible image cropping experience. Made in [Yalantis] (https://yalantis.com/?utm_source=github)

<img src="https://github.com/Yalantis/uCrop/blob/master/preview.png" alt="alt text" style="width:200;height:200">

# Usage

*For a working implementation, please have a look at the Sample Project - sample*

1. Include the library as local library project.

    ``` compile 'com.yalantis:ucrop:1.0.0' ```
    
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

uCrop builder class has method `withOptions(UCrop.Options option)` which extends library configurations.

Currently you can change:

   * image compression format (e.g. PNG, JPEG, WEBP), compression
   * image compression quality [0 - 100]. PNG which is lossless, will ignore the quality setting.
   * whether all gestures are enabled simultaneously
   * maximum size for Bitmap that is decoded from source Uri and used within crop view. If you want to override default behaviour.
   * more coming... (e.g. color pallet) 
    
# Compatibility
  
  * Library - Android GINGERBREAD 2.3+
  * Sample - Android ICS 4.0+
  
# Changelog

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