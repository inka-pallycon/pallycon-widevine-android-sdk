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
