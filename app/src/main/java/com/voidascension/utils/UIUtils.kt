package com.voidascension.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageView
import kotlin.math.max

object UIUtils {
    fun setupRotatedBackground(imageView: ImageView) {
        imageView.post {
            val viewWidth = imageView.width.toFloat()
            val viewHeight = imageView.height.toFloat()
            if (viewWidth <= 0 || viewHeight <= 0) return@post

            val bitmap = BitmapFactory.decodeResource(imageView.resources, com.voidascension.R.drawable.background) ?: return@post
            val bmpWidth = bitmap.width.toFloat()
            val bmpHeight = bitmap.height.toFloat()

            val matrix = Matrix()
            // 1. Rotate 90 degrees
            matrix.postRotate(90f)
            // 2. Translate back into view (after rotation, top-left is at -height, 0)
            matrix.postTranslate(bmpHeight, 0f)

            // 3. Scale to fill (CenterCrop behavior)
            // After 90 deg rotation, new width is bmpHeight, new height is bmpWidth
            val scale = max(viewWidth / bmpHeight, viewHeight / bmpWidth)
            matrix.postScale(scale, scale)

            // 4. Center it
            val dx = (viewWidth - bmpHeight * scale) / 2f
            val dy = (viewHeight - bmpWidth * scale) / 2f
            matrix.postTranslate(dx, dy)

            imageView.setImageBitmap(bitmap)
            imageView.imageMatrix = matrix
        }
    }
}
