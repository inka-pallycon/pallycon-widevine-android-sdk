## Implementation Guide - **PallyCon Widevine SDK** for Android

-------
## **Requirements**

- Android 5.0 (Lollipop) or later
- This SDK has been tested on Gradle 7.2.2, Android Studio Chipmunk and will not work on the simulator.
- This SDK supports ExoPlayer 2.17.1 (contact us about other versions.)
    - Exoplayer 2.11 and earlier versions must be used with PallyConWVSDK v1.15.0.
    - Exoplayer 2.16 and earlier versions must be used with PallyConWVSDK v2.x.x.

## **Note**

- To develop application using the SDK, you should sign up for PallyCon Admin Site to get Site ID and Site Key.

## **Quick Start**

You can add the Pallycon SDK to your development project by following these steps:

1. Extract SDK zip file.

2. Copy `PallyconWVMSDK.aar` file to `project/module/libs/` folder in your project.

3. Apply the below configuration in build.gradle (project).
	
	```gradle
	buildscript {
	    repositories {
	        jcenter()
	        google()
	    }
	    dependencies {
	        classpath "com.android.tools.build:gradle:7.2.2"
	        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10"
	    }
	}
	
	allprojects {
	    repositories {
	        jcenter()
	        google()
	    }
	}
	```

4. Apply the below configuration in build.gradle (app).

	```gradle
	android {
	    defaultConfig {
	        minSdkVersion 21
	        targetSdkVersion 32
	        multiDexEnabled true
	    }
	
	    compileOptions {
	        sourceCompatibility JavaVersion.VERSION_1_8
	        targetCompatibility JavaVersion.VERSION_1_8
	    }
	}
	
	dependencies {
	    implementation 'androidx.core:core-ktx:1.8.0'
        implementation 'com.google.android.material:material:1.6.1'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
        implementation 'androidx.appcompat:appcompat:1.4.2'
        implementation 'androidx.recyclerview:recyclerview:1.2.1'
        implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
        implementation 'androidx.navigation:navigation-fragment-ktx:2.5.3'
        implementation 'androidx.navigation:navigation-ui-ktx:2.5.3'

        implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1'

        // Exo
        implementation "androidx.media3:media3-exoplayer:1.1.1"
        implementation "androidx.media3:media3-ui:1.1.1"
        implementation "androidx.media3:media3-exoplayer-dash:1.1.1"
        implementation "androidx.media3:media3-exoplayer-hls:1.1.1"
        implementation "androidx.media3:media3-exoplayer-rtsp:1.1.1"
        implementation "androidx.media3:media3-exoplayer-smoothstreaming:1.1.1"
        implementation "androidx.media3:media3-datasource-okhttp:1.1.1"
        implementation "androidx.media3:media3-cast:1.1.1"

        // Gson
        implementation 'com.google.code.gson:gson:2.9.1'

        // Secure
        implementation "androidx.security:security-crypto-ktx:1.1.0-alpha03"
	}
	```

5. Implement PallyConEventListener in MainActivity. (Please refer to sample project)

	```kotlin
	val pallyConEventListener: PallyConEventListener = object : PallyConEventListener {
	    override fun onCompleted(currentUrl: String?) {
	        // Called when download is complete: Please refer to the API Guide.
	    }
	
	    override fun onProgress(currentUrl: String?, percent: Float, downloadedBytes: Long) {
	        // Call from start to end of download: Please refer to the API Guide.
	    }
	
	    override fun onStopped(currentUrl: String?) {
	        // Called when download is stopped: Refer to the API Guide.
	    }
	
	    override fun onRestarting(currentUrl: String?) {
	        // Called when download is restarting: Refer to the API Guide.
	    }
	
	    override fun onRemoved(currentUrl: String?) {
	        // Called when downloaded content is removed: Refer to the API Guide.
	    }
	
	    override fun onPaused(currentUrl: String?) {
	        // Called when download is pause: Refer to the API Guide.
	    }
	
	    override fun onFailed(currentUrl: String?, e: PallyConException?) {
	        // Called when an error occurs while downloading content or an error occurs in the license: Refer to the API Guide.
	    }
	
	    override fun onFailed(currentUrl: String?, e: PallyConLicenseServerException?) {
	        // Called when error sent from server when acquiring license: Refer to the API Guide.
	    }
	}
	```

6. Create a PallyConWvSDK object with content information to download. Set the Site ID verified in the PallyCon Admin Site. (Please refer to sample project)

	```kotlin
	
	// Enter DRM related information.
	val config = PallyConDrmConfigration(
	    "site id",
 		"site key", // Set to an empty string if you don't know 
	    "content token",
 		"custom data",
 		mutableMapOf(), // custom header
 		"cookie"
	)
	
	val data = PallyConData(
	    contentId = "content id",
	    url = "content URL",
	    localPath = "Download location where content will be stored",
	    drmConfig = config,
	    cookie = null
	)
	
	val wvSDK = PallyConWvSDK.createPallyConWvSDK(
	    Context, // Context
	    data
	)
	
	wvSDK.setPallyConEventListener(pallyConEventListener)
	```

7. Get the track information of the content to be downloaded. (Please refer to sample project)

	```kotlin
	// The device must be connected to a network.
	// When track information is acquired, the license is also automatically downloaded.
	val trackInfo = wvSDK.getContentTrackInfo()
	```

8. Select the track you want to download from the track information. (Please refer to sample project)

	```kotlin
	// In our sample, we use TrackSelectDialog to select.
	trackInfo.video[0].isDownload = true
	trackInfo.audio[0].isDownload = true
	```

9. Execute the download after checking if the content has already been downloaded. (Please refer to sample project)

	```kotlin
	val state = wvSDK.getDownloadState()
	if (state != COMPLETED) {
	    wvSDK.download(trackInfo)
	}
	```

10. To play downloaded content, obtain a MediaItem or MediaSource using the following API. (Please refer to sample project)

	```kotlin
	// use MediaSource or MediaItem
	val mediaSource = wvSDK.getMediaSource()
	val mediaItem = wvSDK.getMediaItem()
	```

11. Implement player in PlayerActivity.java using the below development guide.
    http://google.github.io/ExoPlayer/guide.html
> Please refer to the below guide from Google for more information about Exoplayer.
> https://developer.android.com/guide/topics/media/exoplayer.html

12. Obtain license duration and playback duration of DRM license.

	```kotlin
	val drmInfo = wvSDK.getDrmInformation()
	val licenseDuration = drmInfo.licenseDuration
	val playbackDuration = drmInfo.playbackDuration
	
	if (licenseDuration <= 0 || playbackDuration <= 0) {
	    // DRM License Expired
	}
	```

13. Set up ExoPlayer as follows. (Please refer to sample project) 

	```kotlin
	ExoPlayer.Builder(this).build()
	    .also { player ->
	        exoPlayer = player
	        binding.exoplayerView.player = player
	        exoPlayer?.setMediaSource(mediaSource) //use mediaSource.
	        exoPlayer?.addListener(object : Player.Listener {
	            override fun onPlayerError(error: PlaybackException) {
	                super.onPlayerError(error)
	            }
	
	            override fun onIsPlayingChanged(isPlaying: Boolean) {
	                super.onIsPlayingChanged(isPlaying)
	            }
	        })
	    }
	```

### **Sign up for download services**
Register a download service based on your customer's situation.
- To support background notifications for downloads, register your download service as shown below.
  ```kotlin
   // DemoDownloadService 는 advanced 샘플을 확인해 주세요.
   wvSDK.setDownloadService(DemoDownloadService::class.java)
   ```

- Register the download service in the AndroidManifest.xml.
  ```xml
  <service
      android:name="com.pallycon.pallyconsample.DemoDownloadService"
      android:exported="false">
      <intent-filter>
          <action android:name="com.google.android.exoplayer.downloadService.action.RESTART" />

          <category android:name="android.intent.category.DEFAULT" />
      </intent-filter>
  </service>
  ```

### **Manage licenses**

You can download and delete licenses regardless of whether you download content.

**Download license**

```kotlin
val uri = Uri.parse("content url")
// val dataSource = FileDataSource.Factory() // local file
val okHttpClient = OkHttpClient.Builder().build()
val dataSource = OkHttpDataSource.Factory(okHttpClient) // remote content
val dashManifest =
    DashUtil.loadManifest(dataSource.createDataSource(), uri)
val format = DashUtil.loadFormatWithDrmInitData(
    dataSource.createDataSource(),
    dashManifest.getPeriod(0)

// The format parameter does not need to be entered unless it is a local file.
// If the format value is NULL, it is automatically defined inside the SDK via the REMOTE CONTENT URL.
wvSDK.downloadLicense(format = format, { 
    Toast.makeText(this@MainActivity, "success download license", Toast.LENGTH_SHORT).show()
}, { e ->
    Toast.makeText(this@MainActivity, "${e.message()}", Toast.LENGTH_SHORT).show()
    print(e.msg)
})
```

**Remove license**

```kotlin
wvSDK.removeLicense()
```

### **Block video recording**

To prevent content leaks with screen recording apps, you should block the capture function by adding the following code to your application:

```kotlin
val view = binding.exoplayerView.videoSurfaceView as SurfaceView
view.setSecure(true)
```

### **Migration for past users**

Since the download method is different from widevine sdk 3.0.0, customers who are using the existing widevine sdk 2.x.x version must migrate the downloaded content.
You can use the "needsMigrateDownloadedContent" function to determine if the content needs to be migrated.
Since the migration function operates only when there is migration content inside, it does not matter if it is called multiple times, and the parameter values of the function should be set identically to the values used in the existing 2.x.x version.
The localPath used when creating the PallyConData object should not be set as the parent directory of the existing downloaded contents. Therefore, if a "MigrationLocalPathException" exception occurs, the localPath value used when creating the PallyConData object must be modified for normal operation.

```kotlin
try {
    if (wvSDK.needsMigrateDownloadedContent(
            url = contents[index].content.url!!,
            contentId = contents[index].cid,
            siteId = contents[index].content.drmConfig!!.siteId!!)
    ) {
        val isSuccess = wvSDK.migrateDownloadedContent(
            url = "", // remote content URL
            contentId = "", // ID of content
            siteId = "", // inputs Site ID which is issued on PallyCon service registration
            contentName = "", // content's name which will be used for the name of downloaded content's folder
            downloadedFolderName = null // content download folder name
        )
    }
} catch (e: PallyConException.MigrationException) {
    print(e)
} catch (e: PallyConException.MigrationLocalPathException) {
    // you have to change localPath
    // ex) val localPath = File(fi, "downloads_v2").toString()
    print(e)
}
```

If the migration is successful, you can delete the 2.x.x version db by yourself like the code below.

```kotlin
val isSuccess = wvSDK.removeOldDownloadedContentDB(
    url = "", // remote content URL
    contentId = "", // ID of content
    siteId = "", // inputs Site ID which is issued on PallyCon service registration
)
```

### **PallyCon Widevine SDK API**

Please refer to the doc/en/api_reference.html file of the SDK zip file for detailed explanation of each API.

