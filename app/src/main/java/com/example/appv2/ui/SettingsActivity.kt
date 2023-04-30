package com.example.appv2.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup
import com.example.appv2.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val themeRadioGroup = findViewById<RadioGroup>(R.id.theme_radio_group)
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.theme_default -> {
                    setAppTheme(Theme.APP)
                }
                R.id.theme_dark -> {
                    setAppTheme(Theme.DARK)
                }
            }
        }
    }

    private fun setAppTheme(theme: Theme) {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("app_theme", theme.name)
        editor.apply()

        // Finish the SettingsActivity and return to MainActivity
        setResult(RESULT_OK)
        finish()
    }

    public enum class Theme {
        APP,
        DARK
    }
}
