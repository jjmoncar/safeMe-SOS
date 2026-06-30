package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.Locale


class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = EmergencyRepository(database.emergencyDao())

    val userProfile = repository.userProfile

    // Selected country and city for preloading reports and numbers
    private val _selectedCountry = MutableStateFlow("Colombia")
    val selectedCountry: StateFlow<String> = _selectedCountry

    private val _selectedCity = MutableStateFlow("Bogotá")
    val selectedCity: StateFlow<String> = _selectedCity

    // Daily reports
    val dailyReports: StateFlow<List<EmergencyReport>> = userProfile.flatMapLatest { profile ->
        if (profile != null) {
            repository.getReportsByLocation(profile.country, profile.city)
        } else {
            repository.getAllReports()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emergency numbers database
    val emergencyNumbers: StateFlow<List<EmergencyNumber>> = userProfile.flatMapLatest { profile ->
        val country = profile?.country ?: "Colombia"
        repository.getEmergencyNumbersForCountry(country)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Screens
    enum class Screen {
        REGISTRATION,
        EMERGENCY_BUTTON,
        REPORTS_DAILY,
        NUMBERS_DB,
        PROFILE
    }

    private val _currentScreen = MutableStateFlow(Screen.EMERGENCY_BUTTON)
    val currentScreen: StateFlow<Screen> = _currentScreen

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Active Emergency State
    private val _activeReport = MutableStateFlow<EmergencyReport?>(null)
    val activeReport: StateFlow<EmergencyReport?> = _activeReport

    // Timer status
    private val _safetyCheckTimer = MutableStateFlow<Int?>(null) // seconds remaining
    val safetyCheckTimer: StateFlow<Int?> = _safetyCheckTimer

    private val _showSafetyDialog = MutableStateFlow(false)
    val showSafetyDialog: StateFlow<Boolean> = _showSafetyDialog

    // Voice Activation State
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _detectedSpeech = MutableStateFlow("")
    val detectedSpeech: StateFlow<String> = _detectedSpeech

    // Email Dispatch Status Logs
    private val _emailLogs = MutableStateFlow<List<String>>(emptyList())
    val emailLogs: StateFlow<List<String>> = _emailLogs

    // Preloaded list of countries and some major cities for registration
    val countriesList = listOf("Argentina", "Chile", "Colombia", "Ecuador", "España", "México", "Perú", "Venezuela", "USA")
    val citiesMap = mapOf(
        "Argentina" to listOf("Buenos Aires", "Córdoba", "Rosario", "Mendoza"),
        "Chile" to listOf("Santiago", "Valparaíso", "Concepción", "La Serena"),
        "Colombia" to listOf("Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena"),
        "Ecuador" to listOf("Quito", "Guayaquil", "Cuenca", "Manta"),
        "España" to listOf("Madrid", "Barcelona", "Valencia", "Sevilla", "Zaragoza"),
        "México" to listOf("CDMX", "Guadalajara", "Monterrey", "Cancún", "Puebla"),
        "Perú" to listOf("Lima", "Arequipa", "Trujillo", "Chiclayo", "Cusco"),
        "Venezuela" to listOf("Caracas", "Maracaibo", "Valencia", "Barquisimeto"),
        "USA" to listOf("Miami", "New York", "San Francisco", "Los Angeles")
    )

    private var timerJob: Job? = null
    private var deadManJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        viewModelScope.launch {
            repository.prepopulateNumbersIfNeeded()
            // Check if profile exists, if not navigate to registration
            userProfile.collect { profile ->
                if (profile == null) {
                    _currentScreen.value = Screen.REGISTRATION
                } else {
                    _selectedCountry.value = profile.country
                    _selectedCity.value = profile.city
                }
            }
        }
    }

    // Register User
    fun registerUser(name: String, identification: String, email: String, familyPhones: String, country: String, city: String) {
        viewModelScope.launch {
            val profile = UserProfile(
                name = name,
                identification = identification,
                email = email,
                familyPhones = familyPhones,
                country = country,
                city = city
            )
            repository.saveProfile(profile)
            _selectedCountry.value = country
            _selectedCity.value = city
            _currentScreen.value = Screen.EMERGENCY_BUTTON
        }
    }

    // Trigger Emergency (Tacto o Voz)
    fun triggerEmergency(alertType: String) {
        viewModelScope.launch {
            val profile = repository.getProfileDirect() ?: return@launch
            val lastReport = _activeReport.value

            val newReport: EmergencyReport
            if (lastReport == null || lastReport.alertType != alertType || lastReport.severityColor == "GREEN") {
                // First activation: YELLOW
                val (lat, lng) = getMockCoordinates(profile.country, profile.city)
                newReport = EmergencyReport(
                    alertType = alertType,
                    timestamp = System.currentTimeMillis(),
                    latitude = lat,
                    longitude = lng,
                    severityColor = "YELLOW",
                    country = profile.country,
                    city = profile.city,
                    lastUpdate = System.currentTimeMillis()
                )
                val id = repository.createReport(newReport)
                _activeReport.value = newReport.copy(id = id.toInt())
                addEmailLog("🚨 SOS Iniciado: Severidad AMARILLA")
                dispatchEmergencySMS(_activeReport.value!!, profile)
                startSafetyCheckTimer()
            } else {
                // Successive trigger
                val nextSeverity = when (lastReport.severityColor) {
                    "YELLOW" -> "ORANGE"
                    "ORANGE" -> "RED"
                    else -> "RED" // Remains RED or goes black on timeout
                }
                newReport = lastReport.copy(
                    severityColor = nextSeverity,
                    lastUpdate = System.currentTimeMillis()
                )
                repository.updateReport(newReport)
                _activeReport.value = newReport
                addEmailLog("⚠️ SOS Actualizado: Severidad $nextSeverity")
                dispatchEmergencySMS(newReport, profile)
                resetSafetyCheckTimer()
            }
        }
    }

    // Update Report to Green (A salvo)
    fun markAsSafe() {
        viewModelScope.launch {
            val report = _activeReport.value ?: return@launch
            val updatedReport = report.copy(
                severityColor = "GREEN",
                lastUpdate = System.currentTimeMillis()
            )
            repository.updateReport(updatedReport)
            _activeReport.value = null // Clear active report state
            stopTimers()
            _showSafetyDialog.value = false
            addEmailLog("✅ Estado Actualizado: A SALVO (Verde). Se envió notificación de tranquilidad a los contactos.")

            val profile = repository.getProfileDirect() ?: return@launch
            // Simulate green SMS dispatch
            sendSafeSMS(updatedReport, profile)
        }
    }

    // Simulate Device Disconnection / Loss of Signal (BLACK code)
    fun simulateDisconnection() {
        viewModelScope.launch {
            val report = _activeReport.value ?: return@launch
            val updatedReport = report.copy(
                severityColor = "BLACK",
                isDisconnected = true,
                lastUpdate = System.currentTimeMillis()
            )
            repository.updateReport(updatedReport)
            _activeReport.value = updatedReport
            stopTimers()
            _showSafetyDialog.value = false
            addEmailLog("💀 ALERTA MÁXIMA: Dispositivo sin señal (Color NEGRO). Alerta enviada automáticamente a los equipos de rescate.")
        }
    }

    // Safety Check Timer logic (Requirement 6)
    private fun startSafetyCheckTimer() {
        stopTimers()
        timerJob = viewModelScope.launch {
            // Wait 15 seconds for simulation demonstration, then prompt
            delay(15000)
            _showSafetyDialog.value = true
            _safetyCheckTimer.value = 15 // 15 seconds to respond
            while (_safetyCheckTimer.value!! > 0) {
                delay(1000)
                _safetyCheckTimer.value = _safetyCheckTimer.value!! - 1
            }
            // If timer runs out without user confirmation, turn BLACK (No signal/unresponsive)
            simulateDisconnection()
        }
    }

    private fun resetSafetyCheckTimer() {
        startSafetyCheckTimer()
    }

    private fun stopTimers() {
        timerJob?.cancel()
        timerJob = null
        deadManJob?.cancel()
        deadManJob = null
        _safetyCheckTimer.value = null
    }

    fun dismissSafetyCheck() {
        _showSafetyDialog.value = false
        // Reset timer to check again later
        startSafetyCheckTimer()
        addEmailLog("🔄 Confirmaste que estás bien. Monitoreo continuo de rescate activo.")
    }

    // Simulated email logs
    private fun addEmailLog(log: String) {
        val current = _emailLogs.value.toMutableList()
        current.add(0, "[${getFormattedTime()}] $log")
        _emailLogs.value = current
    }

    private fun getFormattedTime(): String {
        val date = java.util.Date()
        val format = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    // Coordinate helper
    private fun getMockCoordinates(country: String, city: String): Pair<Double, Double> {
        return when (city) {
            "Bogotá" -> Pair(4.7110, -74.0721)
            "Medellín" -> Pair(6.2442, -75.5812)
            "Cali" -> Pair(3.4516, -76.5320)
            "CDMX" -> Pair(19.4326, -99.1332)
            "Guadalajara" -> Pair(20.6597, -103.3496)
            "Monterrey" -> Pair(25.6866, -100.3161)
            "Lima" -> Pair(-12.0464, -77.0428)
            "Cusco" -> Pair(-13.5320, -71.9675)
            "Santiago" -> Pair(-33.4489, -70.6693)
            "Madrid" -> Pair(40.4168, -3.7038)
            "Barcelona" -> Pair(41.3851, 2.1734)
            "Buenos Aires" -> Pair(-34.6037, -58.3816)
            "Caracas" -> Pair(10.4806, -66.9036)
            "Quito" -> Pair(-0.1807, -78.4678)
            "Miami" -> Pair(25.7617, -80.1918)
            "San Francisco" -> Pair(37.7749, -122.4194)
            else -> Pair(4.0 + (Math.random() - 0.5), -74.0 + (Math.random() - 0.5)) // Random nearby fallback
        }
    }

    // Send SMS Alerts (Simulated & Gemini Drafted)
    private fun dispatchEmergencySMS(report: EmergencyReport, profile: UserProfile) {
        addEmailLog("💬 Iniciando envío de mensajes de texto (SMS) a familiares y rescatistas...")
        viewModelScope.launch {
            val systemTip = getGeminiSystemTip(report.alertType, report.severityColor)
            val phones = profile.familyPhones.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val draft = """
                ===================================================
                🚨 ALERTA DE EMERGENCIA - SAFEME SOS (SMS) 🚨
                ===================================================
                SafeMe - SOS de parte de ${profile.name} (ID: ${profile.identification}).
                
                ⚠️ ESTADO DE RIESGO: Código ${report.severityColor}
                📍 UBICACIÓN: ${profile.city}, ${profile.country}
                🛰️ GPS: Latitud ${report.latitude}, Longitud ${report.longitude}
                🔗 MAPA: https://www.google.com/maps/search/?api=1&query=${report.latitude},${report.longitude}
                
                INCIDENTE: ${report.alertType}.
                
                RECOMENDACIÓN (Gemini AI):
                $systemTip
                ===================================================
            """.trimIndent()

            phones.forEach { phone ->
                addEmailLog("📩 SMS enviado con éxito a $phone [Código ${report.severityColor}]")
            }
            addEmailLog("🚨 Notificación enviada a los cuerpos de seguridad de ${profile.city}, ${profile.country}.")

            // Print AI draft to terminal/log
            Log.d("SafeMe_SOS", draft)
        }
    }

    private fun sendSafeSMS(report: EmergencyReport, profile: UserProfile) {
        val phones = profile.familyPhones.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        phones.forEach { phone ->
            addEmailLog("💬 SMS de TRANQUILIDAD enviado a $phone: ¡${profile.name} está A SALVO!")
        }
    }

    // Call Gemini to generate dynamic Spanish emergency guidelines based on situation
    private suspend fun getGeminiSystemTip(alertType: String, severity: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getStaticFallbackTip(alertType, severity)
        }

        val prompt = "Genera un consejo de rescate ultra conciso en español (máximo 2 líneas) para familiares o rescatistas de una persona que reportó una emergencia tipo \"$alertType\" con severidad \"$severity\" en la app \"SafeMe - SOS\". Sé directo, claro y enfocado en salvar vidas."

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "$prompt"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = okhttp3.Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(okhttp3.RequestBody.create("application/json".toMediaType(), jsonRequest))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""
            if (response.isSuccessful && responseBodyString.isNotEmpty()) {
                val regex = "\"text\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                val matchResult = regex.find(responseBodyString)
                val text = matchResult?.groupValues?.getOrNull(1)
                
                val cleanedText = text
                    ?.replace("\\n", " ")
                    ?.replace("\\\"", "\"")
                    ?.trim()
                
                cleanedText ?: getStaticFallbackTip(alertType, severity)
            } else {
                getStaticFallbackTip(alertType, severity)
            }
        } catch (e: Exception) {
            getStaticFallbackTip(alertType, severity)
        }
    }

    private fun getStaticFallbackTip(alertType: String, severity: String): String {
        return when (alertType) {
            "Terremoto" -> "Manténganse alejados de fachadas de edificios derrumbados. No usen ascensores. Traten de contactarle si hay cobertura celular."
            "Robo", "Asalto" -> "No confronten al delincuente. Denuncien de inmediato al cuadrante policial con las coordenadas geográficas."
            "Tsunami" -> "Evacúen de inmediato hacia zonas elevadas a más de 30 metros sobre el nivel del mar. Sigan rutas de evacuación señalizadas."
            "Ascensor Atrapado" -> "No intenten forzar las puertas del ascensor. Mantengan la calma para dosificar el aire y esperen a los técnicos."
            "Tapiado / Derrumbe" -> "Rescatistas: Usar detectores térmicos o acústicos. Evitar el uso de maquinaria pesada inicialmente para evitar nuevos colapsos."
            else -> "Cuerpos de rescate en camino a las coordenadas adjuntas. Intentar mantener comunicación verbal sin arriesgar la integridad física."
        }
    }

    // Voice Activation: Real SpeechRecognizer Setup with clean fallback/simulated activation
    fun startVoiceListening(context: Context) {
        if (_isListening.value) {
            stopVoiceListening()
            return
        }

        _isListening.value = true
        _detectedSpeech.value = "Escuchando voz..."

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        Log.e("SpeechRecognizer", "Error code: $error")
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _detectedSpeech.value = text
                            processSpeechCommand(text)
                        }
                        _isListening.value = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            }
            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            Log.e("SafeMe", "Speech recognition error: ${e.message}")
            _isListening.value = false
        }
    }

    fun stopVoiceListening() {
        _isListening.value = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // Triggered speech command processing
    fun processSpeechCommand(command: String) {
        val normalized = command.lowercase(Locale.getDefault())
        when {
            normalized.contains("safeme") || normalized.contains("ayuda") || normalized.contains("auxilio") || normalized.contains("socorro") -> {
                triggerEmergency("Otro Grave (Voz)")
            }
            normalized.contains("terremoto") || normalized.contains("sismo") -> {
                triggerEmergency("Terremoto")
            }
            normalized.contains("robo") || normalized.contains("atraco") -> {
                triggerEmergency("Robo")
            }
            normalized.contains("asalto") -> {
                triggerEmergency("Asalto")
            }
            normalized.contains("ascensor") -> {
                triggerEmergency("Ascensor Atrapado")
            }
            normalized.contains("tsunami") -> {
                triggerEmergency("Tsunami")
            }
            normalized.contains("atrapado") || normalized.contains("tapiado") || normalized.contains("derrumbe") -> {
                triggerEmergency("Tapiado / Derrumbe")
            }
        }
    }

    // Clean simulation for Speech input (works perfectly on mock test/emulators)
    fun simulateVoiceCommand(word: String) {
        _detectedSpeech.value = "Comando simulado: \"$word\""
        processSpeechCommand(word)
    }

    fun clearAllReportsData() {
        viewModelScope.launch {
            repository.clearAllReports()
            _activeReport.value = null
            stopTimers()
            _emailLogs.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimers()
        speechRecognizer?.destroy()
    }
}
