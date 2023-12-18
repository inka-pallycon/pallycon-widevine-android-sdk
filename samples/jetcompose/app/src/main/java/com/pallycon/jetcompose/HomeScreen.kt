package com.pallycon.jetcompose

import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.gson.Gson
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.model.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalViewModel = compositionLocalOf<HomeViewModel> { error("No ViewModel found!") }

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = HomeViewModel()) {
    val context = LocalContext.current
    val contents by viewModel.contents.collectAsState()

    Surface {
        viewModel.initialize(context)
        viewModel.prepare()
        CompositionLocalProvider(LocalViewModel provides viewModel) {
            ContentList(
                contentDatas = contents,
                navController = navController
            )
        }
    }
}

@Composable
fun ContentList(
    contentDatas: List<ContentData>,
    navController: NavController,
) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        items(contentDatas) { video ->
            ContentListItem(
                contentData = video,
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentListItem(
    contentData: ContentData,
    navController: NavController,
) {
    val context = LocalContext.current
    val viewModel = LocalViewModel.current
    val dialogState = remember { mutableStateOf(false) }

    if (dialogState.value) {
        Dialog(onDismissRequest = { dialogState.value = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(375.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                val wvSDK = contentData.wvSDK
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Delete Menu",
                        modifier = Modifier.padding(16.dp),
                    )
                    TextButton(onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            wvSDK.downloadLicense(null, onSuccess = {
                                Toast.makeText(
                                    context,
                                    "success download license",
                                    Toast.LENGTH_LONG
                                ).show()
                            }, onFailed = { e ->
                                Toast.makeText(context, "${e.message()}", Toast.LENGTH_LONG).show()
                                print(e.msg)
                            })
                        }
                    }) { Text(text = "download license") }

                    TextButton(onClick = {
                        try {
                            wvSDK.renewLicense()
                        } catch (e: PallyConException.DrmException) {
                            Toast.makeText(context, "${e.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(text = "renew license") }

                    TextButton(onClick = {
                        try {
                            wvSDK.removeLicense()
                        } catch (e: PallyConException.DrmException) {
                            Toast.makeText(context, "${e.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(text = "remove license") }

                    TextButton(onClick = {
                        wvSDK.removeAll()
                        viewModel.prepare()
                    }) { Text(text = "remove all") }

                    TextButton(onClick = {
                        val info = wvSDK.getDrmInformation()
                        val alertBuilder = AlertDialog.Builder(context)
                        alertBuilder.setTitle("drm license info")
                            .setMessage(
                                "licenseDuration : ${info.licenseDuration} \n" +
                                        "playbackDuration : ${info.playbackDuration}"
                            )
                        alertBuilder.setNegativeButton("Cancel", null)
                        alertBuilder.show()
                    }) { Text(text = "license info") }

                    TextButton(onClick = {
                        val info = wvSDK.getDownloadFileInformation()
                        val alertBuilder = AlertDialog.Builder(context)
                        alertBuilder.setTitle("drm license info")
                            .setMessage(
                                "downloaded size : ${info.downloadedFileSize} \n"
                            )
                        alertBuilder.setNegativeButton("Cancel", null)
                        alertBuilder.show()
                    }) { Text(text = "downloaded file info") }

                    TextButton(onClick = {
                        var keySetId = wvSDK.getKeySetId()
                        val alertBuilder = AlertDialog.Builder(context)
                        alertBuilder.setTitle("KetSetId")
                            .setMessage(
                                "KeySetId : ${keySetId}"
                            )
                        alertBuilder.setNegativeButton("Cancel", null)
                        alertBuilder.show()
                    }) { Text(text = "KeySetId") }

                    TextButton(onClick = {
                        wvSDK.reProvisionRequest({}, { e ->
                            print(e.message())
                        })
                    }) { Text(text = "re-provisioning") }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .padding(top = 8.dp)
            .height(80.dp)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .combinedClickable(
                    onClick = {
                        val json = Uri.encode(Gson().toJson(contentData.content))
                        navController.navigate("player/${json}")
//                        navController.navigate("player/${contentData.content}")
//                        val id = "android-app://androidx.navigation/player/{PallyConData}".hashCode()
//                        navController.navigate(
//                            resId = id, // internalRoute.hashCode()
//                            args = bundleOf(
//                                "PallyConData" to contentData.content
//                            )
//                        )
                    },
                    onLongClick = {
                        dialogState.value = true
                    }
                )

        ) {
            Column {
                Text(
                    text = contentData.title,
                    modifier = Modifier
                        .padding(end = 8.dp),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = contentData.subTitle,
                    modifier = Modifier
                        .padding(end = 8.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (contentData.downloadTracks == null && contentData.status == DownloadState.NOT) {
                Text(text = "preparing..")
            } else {
                val imageResourceId = when (contentData.status) {
                    DownloadState.COMPLETED -> {
                        R.drawable.baseline_delete_24
                    }

                    DownloadState.DOWNLOADING -> {
                        R.drawable.baseline_pause_circle_outline_24
                    }

                    else -> {
                        R.drawable.baseline_arrow_downward_24
                    }
                }

                Image(
                    painter = painterResource(id = imageResourceId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            when (contentData.status) {
                                DownloadState.COMPLETED -> {
                                    viewModel.remove(contentData)
                                }

                                DownloadState.DOWNLOADING -> {
                                    viewModel.pauseAll()
                                }

                                DownloadState.PAUSED -> {
                                    viewModel.resumeAll()
                                }

                                else -> {
                                    contentData.downloadTracks?.let {
                                        viewModel.download(contentData, it)
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}