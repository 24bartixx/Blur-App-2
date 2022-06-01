package com.example.background.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R


/**
 * Worker to blur the cupcake image
 */

private const val TAG = "BlurWorker"

class BlurWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    override fun doWork(): Result {

        val appContext = applicationContext

        // get input Uri of the cupcake image
        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        makeStatusNotification("Blurring the image", appContext)

        // slow down the work, so that it's easier to see each WorkRequest start
        sleep()

        return try {
            // create bitmap from the cupcake image
            // (CODE) val picture = BitmapFactory.decodeResource(appContext.resources, R.drawable.android_cupcake)

            // check that resourceUri is not empty
            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input Uri")
                throw IllegalArgumentException("Invalid input uri")
            }

            // create bitmap from the cupcake image
            val picture = BitmapFactory.decodeStream(appContext.contentResolver.openInputStream(Uri.parse(resourceUri)))


            // get a blurred version of the bitmap
            val output = blurBitmap(picture, appContext)

            // write bitmap to temporary file and return the Uri
            val outputUri = writeBitmapToFile(appContext, output)

            makeStatusNotification("Output is $outputUri", appContext)

            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            return Result.success(outputData)

        } catch(throwable: Throwable) {
            Log.e(TAG, "Error applying blur")
            throwable.printStackTrace()
            Result.failure()
        }
    }

}