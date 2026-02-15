package com.novexel_studio.newapp


import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.novexel_studio.newapp.databinding.ActivityMainBinding
import net.objecthunter.exp4j.ExpressionBuilder
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isNewOp = true
    private val maxInputLength = 20 // Reduced for better UI fitting

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Sound
        SoundPlayer.init()
        switchScreen(false)
        // Groups for logic
        val numberButtons = listOf(
            binding.no0, binding.no1, binding.no2, binding.no3, binding.no4,
            binding.no5, binding.no6, binding.no7, binding.no8, binding.no9
        )

        val opButtons = listOf(
            binding.Addition, binding.Subtraction,
            binding.Multiplication, binding.divide, binding.modulas
        )

        // Number Input Logic
        numberButtons.forEach { button ->
            button.setOnClickListener {
                if (isNewOp || binding.tvDisplay.text.toString() == "0") {
                    binding.tvDisplay.text = ""
                    isNewOp = false
                }
                if (binding.tvDisplay.length() < maxInputLength) {
                    binding.tvDisplay.append(button.text)
                    SoundPlayer.playVib(it) // Using the playTap from earlier
                    updatePreview()
                    scrollTextToEnd()
                }
                binding.tvDisplay.movementMethod = android.text.method.ScrollingMovementMethod()
                binding.tvDisplay.isSelected = true
            }
        }

        // Operator Input Logic
        opButtons.forEach { button ->
            button.setOnClickListener {
                val current = binding.tvDisplay.text.toString()
                if (current.isNotEmpty()) {
                    isNewOp = false
                    // If last char is an operator, replace it instead of adding another
                    if (isLastCharOperator(current)) {
                        binding.tvDisplay.text = current.dropLast(1) + button.text
                    } else {
                        binding.tvDisplay.append(button.text)
                    }
                    SoundPlayer.playVib(it)
                    scrollTextToEnd()
                }
            }
        }

        // Dot Logic (Prevents 5.5.5)
        binding.dot.setOnClickListener {
            val current = binding.tvDisplay.text.toString()
            val lastNumber = current.split('+', '−', '×', '÷').last()
            if (!lastNumber.contains(".")) {
                binding.tvDisplay.append(".")
                isNewOp = false
                SoundPlayer.playVib(it)
            }
        }

        binding.AC.setOnClickListener {
            binding.tvDisplay.text = "0"
            binding.tvPreview.text = ""
            isNewOp = true

            SoundPlayer.playVib(it)
        }

        // Equal Button (Note: check if your ID is equal_to or Calculate)
        binding.Calculate.setOnClickListener {
            val expression = binding.tvDisplay.text.toString()
            val result = evaluate(expression)
            if (result.isNotEmpty()) {
                binding.tvDisplay.text = result
                binding.tvPreview.text = ""
                isNewOp = true
                SoundPlayer.playVib(it)
            }
        }

        // Plus/Minus Toggle
        binding.pOrm.setOnClickListener {
            val current = binding.tvDisplay.text.toString()
            if (current != "0" && current.isNotEmpty()) {
                if (current.startsWith("-")) {
                    binding.tvDisplay.text = current.substring(1)
                } else {
                    binding.tvDisplay.text = "-$current"
                }
                updatePreview()
                SoundPlayer.playVib(it)
            }
        }
        val handler = Handler(Looper.getMainLooper())

// 1. Create the repeatable delete task
        val continuousDelete = object : Runnable {
            override fun run() {
                val current = binding.tvDisplay.text.toString()
                if (current.isNotEmpty() && current != "0") {
                    val nextText = current.dropLast(1)
                    binding.tvDisplay.text = if (nextText.isEmpty()) "0" else nextText
                    binding.delete.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    // Repeat this every 100 milliseconds
                    updatePreview()
                    handler.postDelayed(this, 100)
                }
            }
        }

// 2. Handle the Touch Events (Down = Start, Up = Stop)
                binding.delete.setOnTouchListener { v, event ->
                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {
                            // Wait 500ms before starting the "rapid fire" delete
                            handler.postDelayed(continuousDelete, 500)

                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // Stop deleting immediately when finger is lifted
                            handler.removeCallbacks(continuousDelete)
                        }


                    }
                    // Return 'false' so the normal setOnClickListener still works for single taps!
                    false
                }

// 3. Keep your existing Single Tap logic
        binding.delete.setOnClickListener {
            val current = binding.tvDisplay.text.toString()
            if (current.isNotEmpty() && current != "0") {
                val nextText = current.dropLast(1)
                binding.tvDisplay.text = if (nextText.isEmpty()) "0" else nextText

            }
            updatePreview()
            SoundPlayer.playVib(it)
        }

        binding.activeCalculator.setOnClickListener { switchScreen(false)
            SoundPlayer.playVib(it)}
        binding.activeCalculatorpercentage.setOnClickListener { switchScreen(true)
            SoundPlayer.playVib(it)}
        binding.btnCalculatePerc.setOnClickListener { calculatePercentage()
            SoundPlayer.playVib(it) }

    }
    private fun scrollTextToEnd() {
        val scrollAmount = binding.tvDisplay.layout?.getLineRight(0)?.toInt() ?: 0
        val viewWidth = binding.tvDisplay.width
        if (scrollAmount > viewWidth) {
            binding.tvDisplay.scrollTo(scrollAmount - viewWidth, 0)
        }
    }

    private fun isLastCharOperator(text: String): Boolean {
        if (text.isEmpty()) return false
        val last = text.last().toString()
        return last in listOf("+", "−", "×", "÷", "%")
    }

    private fun updatePreview() {
        val text = binding.tvDisplay.text.toString()
        if (text.isEmpty() || isLastCharOperator(text)) {
            binding.tvPreview.text = ""
            return
        }
        val result = evaluate(text)
        binding.tvPreview.text = if (result != text) result else ""
    }


    private fun switchScreen(showPercentage: Boolean) {
        val calcGroup = binding.calculatorGroup
        val percArea = binding.percentageArea

        if (showPercentage) {
            // Fade out calculator, Fade in percentage
            calcGroup.animate().alpha(0f).setDuration(200).withEndAction {
                calcGroup.visibility = View.GONE
                percArea.visibility = View.VISIBLE
                percArea.alpha = 0f
                percArea.animate().alpha(1f).setDuration(200).start()
            }
        } else {
            // Fade out percentage, Fade in calculator
            percArea.animate().alpha(0f).setDuration(200).withEndAction {
                percArea.visibility = View.GONE
                calcGroup.visibility = View.VISIBLE
                calcGroup.alpha = 0f
                calcGroup.animate().alpha(1f).setDuration(200).start()
            }
        }
    }
    @SuppressLint("DefaultLocale")


    private  fun calculatePercentage(){
        val number = binding.etNumber.text.toString().toDoubleOrNull()
        val part = binding.etPercentage.text.toString().toDoubleOrNull()
        var result:Double = 0.0
        val df = DecimalFormat("###.##")
        if (number != null && part != null  && number >0 && part >0)
        {
            result = (number * part) / 100


        }

        binding.tvResult.text = df.format(result).toString()
    }
    private fun evaluate(expression: String): String {
        return try {
            val clean = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")

            val exp = ExpressionBuilder(clean).build()
            val result = exp.evaluate()

            when {
                // If result is huge, use scientific notation (e.g., 1.2E10)
                result > 999999999 || result < -999999999 -> {
                    String.format("%.6e", result)
                }
                // If it's a whole number
                result % 1 == 0.0 -> {
                    result.toLong().toString()
                }
                // Normal decimal
                else -> {
                    String.format("%.8f", result)
                        .trimEnd('0')
                        .trimEnd('.')
                }
            }
        } catch (e: Exception) {
            ""
        }



    }
}