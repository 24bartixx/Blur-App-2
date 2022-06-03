/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : ViewModel() {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    // create class variable for a WorkManager
    // WorkManager - schedules WorkRequests and makes it run (spreads out system resources, honours constraints)
    private val workManager = WorkManager.getInstance(application)

    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    init {
        imageUri = getImageUri(application.applicationContext)
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }
    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {

        /**  (INFO) Work request for one worker (BlurWorker)
            val blurRequest = OneTimeWorkRequestBuilder<BlurWorker>().setInputData(createInputDataForUri()).build()

            workManager.enqueue(blurRequest)
        */

        // (CODE) var continuation = workManager.beginWith(OneTimeWorkRequest.from(CleanupWorker::class.java))
        var continuation = workManager.beginUniqueWork(IMAGE_MANIPULATION_WORK_NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequest.from(CleanupWorker::class.java))

        for (i in 0 until blurLevel) {
            /** (INFO) blurRequest for one Worker instance performed
                val blurRequest = OneTimeWorkRequest.Builder(BlurWorker::class.java)
                    .setInputData(createInputDataForUri()).build()

                continuation = continuation.then(blurRequest)
            */

            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            if(i==0) blurBuilder.setInputData(createInputDataForUri())

            continuation = continuation.then(blurBuilder.build())

        }



        // (CODE) val saveRequest = OneTimeWorkRequest.Builder(SaveImageToFileWorker::class.java).build()
        val saveRequest = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .addTag(TAG_OUTPUT)
            .build()

        continuation = continuation.then(saveRequest)

        continuation.enqueue()

    }

    private fun createInputDataForUri(): Data {

        val dataBuilder = Data.Builder()

        imageUri?.let {
            dataBuilder.putString(KEY_IMAGE_URI, imageUri.toString())
        }

        return dataBuilder.build()

    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
