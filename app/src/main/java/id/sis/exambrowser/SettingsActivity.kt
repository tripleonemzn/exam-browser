package id.sis.exambrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import id.sis.exambrowser.databinding.ActivitySettingsBinding

class SettingsActivity : ComponentActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentBaseUrl = ExamConfig.getBaseUrl(this)
        binding.baseUrlInput.setText(currentBaseUrl)

        binding.saveButton.setOnClickListener {
            val url = binding.baseUrlInput.text?.toString().orEmpty()
            if (url.isNotBlank()) {
                ExamConfig.setBaseUrl(this, url)
                finish()
            }
        }
    }
}

