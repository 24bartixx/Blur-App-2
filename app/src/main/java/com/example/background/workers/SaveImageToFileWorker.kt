package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SaveImageToFileWorker"

class SaveImageToFileWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override fun doWork(): Result {

        // code provided in the codelab (file manipulation is out of scope of the codelab)
        // save blurred file to image and return its Uri
        makeStatusNotification("Saving image", applicationContext)
        // slow down the work, so that it's easier to see each WorkRequest start
        sleep()

        val resolver = applicationContext.contentResolver
        return try {

            val resourceUri = inputData.getString(KEY_IMAGE_URI)
            val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)))
            val imageUrl = MediaStore.Images.Media.insertImage( resolver, bitmap, title, dateFormatter.format(Date()) )

            if (!imageUrl.isNullOrEmpty()) {
                val output = workDataOf(KEY_IMAGE_URI to imageUrl)
                Result.success()
            } else {
                Log.e(TAG, "Writing to MediaStore failed")
                Result.failure()
            }

        } catch(exception: Exception) {
            exception.printStackTrace()
            Result.failure()
        }

    }
}