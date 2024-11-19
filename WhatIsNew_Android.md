# Version 4.3.2

- **Added `ClearKeyLicenseException` exception:**
  - Added an error that occurs when attempting to download a license for clearkey or non-DRM content.

- **Updated `download()` function:**
  - downloads of Clearkey or NonDRM content will proceed even if you don't have a license to download it.

- **Updated `getMediaSource()` and `getMediaItem` function:**
  - When calling the function while downloading, get the media of the content being downloaded.

# Version 4.3.1

- **Updated Libraries:**
    - media3: Updated to `1.4.1`

# Version 4.3.0

- **Support for license policy 2.0:**
    - The SDK now processes license data according to policy 2.0 specifications.
    - `setPallyConCallback()`, `setDownloadService()` and `getDownloadManager()` have been changed to static functions.
    - Use `PallyConWvSDK.setPallyConCallback()` from now on.
    - The old `setPallyConCallback()` and `setDownloadService()` functions are deprecated.

- **Event listener functions added:**
    - `addPallyConEventListener()` and `removePallyConEventListener()` functions are now available.
    - You can register and remove `PallyConEventListener`.
    - The old `setPallyConEventListener()` function is deprecated.

- **New setCmcdConfigurationFactory() function:**
    - Configure Common Media Client Data (CMCD) for your CDN real-time logs.

- **Updated `PallyConCallback` interface:**
    - The `executeKeyRequest` function parameter has been changed from `url` to `contentData`.

- **Updated Libraries:**
    - core: Updated to `1.13.1`
    - appCompat: Updated to `1.7.0`
    - material: Updated to `1.12.0`
    - coroutines: Updated to `1.8.1`
    - media3: Updated to `1.3.1`
    - gson: Updated to `2.11.0`
    - security: Updated to `1.1.0-alpha06`

# Version 4.2.0

- **New stop() function added:**
  - Content downloads can now be interrupted.
  - Interrupted downloads can be resumed later.

- **Changes to PallyConEventListener event listeners:**
  - The `contentUrl` parameter has been replaced with `ContentData(contentId, url, ..., drmConfig)`.

- **ContentData class modifications:**
  - The `localPath` parameter has been removed.
  - Added `setDownloadDirectory()` static function to set the download directory.
  - Separate directories for each content are no longer supported (this matches the behavior of the old code).

# Version 4.1.0

>- PallyConSDK has been updated to version 4.1.0.
>  - Fixed redownload not happening if remove after pause

# Version 4.0.1

>- PallyConSDK has been updated to version 4.0.1.
   >  - Added a "getDrmSessionManager" function.
   >  - Added "getMediaSource" function that utilizes the drmSessionManager parameter.
   >    -  You can specify a drmSessionManager to create the mediaSource object.
   >  - Fixed an issue that caused the download() function to crash when running in the main thread from now on.
   >    - A function runs as a background thread under the hood.
   >  - Fixed crash when selecting more than one video track

# Version 4.0.0

>- PallyConSDK has been updated to version 4.0.0
   >  - Changed the ExoPlayer Package -> Media3 Package

# Version 3.4.6

>- PallyConSDK has been updated to version 3.4.6.
   >  - Fixed an issue where the license key rotation feature for live content was not working correctly.

# Version 3.4.5

>- PallyConSDK has been updated to version 3.4.5.
   >  - Added a PallyConLicenseCipherException.

# Version 3.4.3

>- PallyConSDK has been updated to version 3.4.3.
   >  - Fixed an internal error.

# Version 3.4.2

>- PallyConSDK has been updated to version 3.4.2.
   >  - Starting with 3.4.2, DB migration between 2.X.X versions is not supported.
>  - Removed "removeOldDownloadedContentDB" function

# Version 3.4.1

>- PallyConSDK has been updated to version 3.4.1.
   >  - Fixed the LicenseCipher feature part.

# Version 3.4.0

> - PallyConSDK has been updated to version 3.4.0. 
>   - Added contentId member variable to "ContentData" class.
>     - For content management, the contentId used in the 2.x.x version range has been added back..
>       If you don't enter a contentId, the content will be managed by url. 
>   - We've changed how we handle the migration of historical content.
>     - Added "needsMigrateDownloadedContent" function to check whether the content needs to be migrated or not. If that function returns true, you can run the "migrateDownloadedContent" function.
>     - Added "removeOldDownloadedContentDB" function can remove the DB of content that was downloaded before the SDK 2.x.x version. 
>   - Added a format parameter to the "downloadLicense" function. 
>     - If it's null, the format value is taken directly from the content url(remote content) internally to download the license.
>   - Added a "setDownloadService" and "getDownloadManager" functions.
>     - Can now turn off the default applied alarm and use the DownloadService set by the customer.
>     - Can add their own using the setDownloadService function.
>     - Additionally, the DownloadManager object needed to create the DownloadService can now be retrieved using the getDownloadManager function.
>   - Added licenseCipherPath member variable to "PallyConDrmConfigration" class
>     - This is a member variable added for customers using PallyCon LicenseCipher and has a default value of null.
>     - For more information, please contact us at PallyCon.

# Version 3.3.0

> - PallyConSDK has been updated to version 3.3.0.
>   - Added "setPallyConCallback" function and "PallyConCallback" interface
>     - You can handle the communication part with the server when getting the license as a callback.
>     - In the callback, you can further proceed with things like encrypting the data. 
>   - Modified HLS download.

# Version 3.2.0

> - PallyConSDK has been updated to version 3.2.0. 
>   - Added download and play for HLS(m3u8) widevine contents 
>   - Added download for Non-DRM contents

# Version 3.1.0

> - PallyConSDK has been updated to version 3.1.0. 
>   - Added migration function "migrate Downloaded Content" for users existing 2.x versions. 
>   - Changed "createPallyConWvSDK" function. instead of entering a PallyConEventListener object as a "createPallyConWvSDK" parameter, set it using the "setPallyConEventListener" function. 
>   - Existing 2.x.x customers must call in advance. 

# Version 3.0.1

> - PallyConSDK has been updated to version 3.0.1. 
>   - Bug fix, Crash occurs when the getContentTrackInfo() function is called multiple times while offline

# Version 3.0.0

> - PallyConSDK has been updated to version 3.0.0. 
>   - PallyConSDK 3.0.0 is based on Kotlin and can be used in java. 
>   - All of the APIs used in the previous 2.x.x version have been changed. 
>   - From now on, background multi-download (max 6) is supported.
