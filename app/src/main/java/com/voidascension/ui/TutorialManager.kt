package com.voidascension.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import com.voidascension.R
import com.voidascension.data.SaveManager
import com.google.android.material.button.MaterialButton

class TutorialManager(private val saveManager: SaveManager) {

    data class TutorialStep(
        val message: String,
        val targetViewId: Int? = null,
        val xPercent: Float? = null, // for canvas elements
        val yPercent: Float? = null
    )

    fun showTutorial(activity: Activity, tutorialId: String, steps: List<TutorialStep>, onComplete: () -> Unit = {}) {
        if (saveManager.isTutorialCompleted(tutorialId)) {
            onComplete()
            return
        }

        var currentStepIndex = 0

        val dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_tutorial, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#CC000000")))

        val tvMessage = view.findViewById<TextView>(R.id.tvTutorialMessage)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnTutorialNext)
        val highlightFrame = view.findViewById<View>(R.id.highlightFrame)

        fun updateStep() {
            val step = steps[currentStepIndex]
            tvMessage.text = step.message
            
            if (currentStepIndex == steps.size - 1) {
                btnNext.text = "GOTH IT"
            } else {
                btnNext.text = "NEXT"
            }

            // Position highlight if targetViewId is provided
            step.targetViewId?.let { id ->
                val target = activity.findViewById<View>(id)
                if (target != null) {
                    val location = IntArray(2)
                    target.getLocationOnScreen(location)
                    highlightFrame.visibility = View.VISIBLE
                    val params = highlightFrame.layoutParams as ViewGroup.MarginLayoutParams
                    params.leftMargin = location[0] - 10
                    params.topMargin = location[1] - 10
                    params.width = target.width + 20
                    params.height = target.height + 20
                    highlightFrame.layoutParams = params
                } else {
                    highlightFrame.visibility = View.GONE
                }
            } ?: run {
                // If xPercent and yPercent are provided (for canvas elements)
                if (step.xPercent != null && step.yPercent != null) {
                    highlightFrame.visibility = View.VISIBLE
                    val w = activity.window.decorView.width
                    val h = activity.window.decorView.height
                    val params = highlightFrame.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = (w * 0.2f).toInt()
                    params.height = (h * 0.1f).toInt()
                    params.leftMargin = (w * step.xPercent - params.width / 2).toInt()
                    params.topMargin = (h * step.yPercent - params.height / 2).toInt()
                    highlightFrame.layoutParams = params
                } else {
                    highlightFrame.visibility = View.GONE
                }
            }
        }

        btnNext.setOnClickListener {
            currentStepIndex++
            if (currentStepIndex < steps.size) {
                updateStep()
            } else {
                saveManager.markTutorialCompleted(tutorialId)
                dialog.dismiss()
                onComplete()
            }
        }

        updateStep()
        dialog.show()
    }
}
