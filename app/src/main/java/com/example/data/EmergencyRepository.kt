package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EmergencyRepository(private val dao: EmergencyDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfileFlow()

    fun getAllReports(): Flow<List<EmergencyReport>> = dao.getAllReportsFlow()

    fun getReportsByLocation(country: String, city: String): Flow<List<EmergencyReport>> =
        dao.getReportsByLocationFlow(country, city)

    suspend fun getProfileDirect(): UserProfile? = dao.getUserProfile()

    suspend fun saveProfile(profile: UserProfile) {
        dao.insertUserProfile(profile)
    }

    suspend fun createReport(report: EmergencyReport): Long {
        return dao.insertReport(report)
    }

    suspend fun updateReport(report: EmergencyReport) {
        dao.updateReport(report)
    }

    suspend fun getReportById(id: Int): EmergencyReport? {
        return dao.getReportById(id)
    }

    suspend fun clearAllReports() {
        dao.deleteAllReports()
    }

    fun getEmergencyNumbersForCountry(country: String): Flow<List<EmergencyNumber>> =
        dao.getEmergencyNumbersByCountryFlow(country)

    suspend fun prepopulateNumbersIfNeeded() {
        val count = dao.getEmergencyNumbersCount()
        if (count == 0) {
            val defaultNumbers = listOf(
                // Colombia
                EmergencyNumber(country = "Colombia", category = "Policía", number = "123"),
                EmergencyNumber(country = "Colombia", category = "Bomberos", number = "119"),
                EmergencyNumber(country = "Colombia", category = "Ambulancia / Urgencias", number = "125"),
                EmergencyNumber(country = "Colombia", category = "Defensa Civil", number = "144"),
                EmergencyNumber(country = "Colombia", category = "Cruz Roja", number = "132"),

                // México
                EmergencyNumber(country = "México", category = "Emergencias Nacional", number = "911"),
                EmergencyNumber(country = "México", category = "Bomberos", number = "068"),
                EmergencyNumber(country = "México", category = "Cruz Roja", number = "065"),
                EmergencyNumber(country = "México", category = "Policía Federal", number = "088"),

                // Perú
                EmergencyNumber(country = "Perú", category = "Policía Nacional", number = "105"),
                EmergencyNumber(country = "Perú", category = "Cuerpo de Bomberos", number = "116"),
                EmergencyNumber(country = "Perú", category = "Ambulancia (SAMU)", number = "106"),
                EmergencyNumber(country = "Perú", category = "Cruz Roja", number = "115"),
                EmergencyNumber(country = "Perú", category = "Defensa Civil", number = "110"),

                // España
                EmergencyNumber(country = "España", category = "Emergencias", number = "112"),
                EmergencyNumber(country = "España", category = "Policía Nacional", number = "091"),
                EmergencyNumber(country = "España", category = "Guardia Civil", number = "062"),
                EmergencyNumber(country = "España", category = "Urgencias Médicas", number = "061"),
                EmergencyNumber(country = "España", category = "Bomberos", number = "080"),

                // Argentina
                EmergencyNumber(country = "Argentina", category = "Emergencias", number = "911"),
                EmergencyNumber(country = "Argentina", category = "Policía", number = "101"),
                EmergencyNumber(country = "Argentina", category = "Bomberos", number = "100"),
                EmergencyNumber(country = "Argentina", category = "Emergencia Médica", number = "107"),

                // Chile
                EmergencyNumber(country = "Chile", category = "Carabineros (Policía)", number = "133"),
                EmergencyNumber(country = "Chile", category = "Cuerpo de Bomberos", number = "132"),
                EmergencyNumber(country = "Chile", category = "Ambulancia (SAMU)", number = "131"),
                EmergencyNumber(country = "Chile", category = "Socorro Andino", number = "136"),

                // Ecuador
                EmergencyNumber(country = "Ecuador", category = "Emergencias (ECU 911)", number = "911"),
                EmergencyNumber(country = "Ecuador", category = "Policía", number = "101"),
                EmergencyNumber(country = "Ecuador", category = "Bomberos", number = "102"),
                EmergencyNumber(country = "Ecuador", category = "Cruz Roja", number = "131"),

                // Venezuela
                EmergencyNumber(country = "Venezuela", category = "Emergencias Nacional", number = "911"),
                EmergencyNumber(country = "Venezuela", category = "Bomberos", number = "171"),
                EmergencyNumber(country = "Venezuela", category = "Tránsito Terrestre", number = "0800-8726748"),
                EmergencyNumber(country = "Venezuela", category = "Protección Civil", number = "0800-7248451"),

                // USA
                EmergencyNumber(country = "USA", category = "Emergency (Police/Fire/Ambulance)", number = "911"),

                // Bolivia
                EmergencyNumber(country = "Bolivia", category = "Policía", number = "110"),
                EmergencyNumber(country = "Bolivia", category = "Bomberos", number = "119"),
                EmergencyNumber(country = "Bolivia", category = "Ambulancia", number = "118"),

                // Brasil
                EmergencyNumber(country = "Brasil", category = "Policia Militar", number = "190"),
                EmergencyNumber(country = "Brasil", category = "Bombeiros", number = "193"),
                EmergencyNumber(country = "Brasil", category = "Ambulância (SAMU)", number = "192"),

                // Canadá
                EmergencyNumber(country = "Canadá", category = "Emergency (Police/Fire/Ambulance)", number = "911"),

                // Costa Rica
                EmergencyNumber(country = "Costa Rica", category = "Emergencias", number = "911"),

                // Cuba
                EmergencyNumber(country = "Cuba", category = "Policía", number = "106"),
                EmergencyNumber(country = "Cuba", category = "Bomberos", number = "105"),
                EmergencyNumber(country = "Cuba", category = "Ambulancia", number = "104"),

                // El Salvador
                EmergencyNumber(country = "El Salvador", category = "Emergencias", number = "911"),

                // Guatemala
                EmergencyNumber(country = "Guatemala", category = "Policía Nacional Civil", number = "110"),
                EmergencyNumber(country = "Guatemala", category = "Bomberos Voluntarios", number = "122"),
                EmergencyNumber(country = "Guatemala", category = "Cruz Roja", number = "125"),

                // Honduras
                EmergencyNumber(country = "Honduras", category = "Emergencias", number = "911"),

                // Nicaragua
                EmergencyNumber(country = "Nicaragua", category = "Policía Nacional", number = "118"),
                EmergencyNumber(country = "Nicaragua", category = "Bomberos", number = "115"),
                EmergencyNumber(country = "Nicaragua", category = "Cruz Roja", number = "128"),

                // Panamá
                EmergencyNumber(country = "Panamá", category = "Emergencias / Policía", number = "104"),
                EmergencyNumber(country = "Panamá", category = "Bomberos", number = "103"),
                EmergencyNumber(country = "Panamá", category = "Cruz Roja / Sume", number = "911"),

                // Paraguay
                EmergencyNumber(country = "Paraguay", category = "Sistema de Emergencias", number = "911"),
                EmergencyNumber(country = "Paraguay", category = "Bomberos", number = "132"),

                // Puerto Rico
                EmergencyNumber(country = "Puerto Rico", category = "Emergencias", number = "911"),

                // República Dominicana
                EmergencyNumber(country = "República Dominicana", category = "Sistema Nacional de Emergencias", number = "911"),

                // Uruguay
                EmergencyNumber(country = "Uruguay", category = "Emergencias", number = "911"),

                // Reino Unido
                EmergencyNumber(country = "Reino Unido", category = "Emergency (Police/Fire/Ambulance)", number = "999"),

                // Francia
                EmergencyNumber(country = "Francia", category = "Urgences (Emergencies)", number = "112"),

                // Alemania
                EmergencyNumber(country = "Alemania", category = "Notruf (Emergencies)", number = "112"),

                // Italia
                EmergencyNumber(country = "Italia", category = "Emergenza (Emergencies)", number = "112"),

                // Portugal
                EmergencyNumber(country = "Portugal", category = "Emergência", number = "112"),

                // Japón
                EmergencyNumber(country = "Japón", category = "Police (110)", number = "110"),
                EmergencyNumber(country = "Japón", category = "Ambulance / Fire (119)", number = "119"),

                // China
                EmergencyNumber(country = "China", category = "Police (110)", number = "110"),
                EmergencyNumber(country = "China", category = "Ambulance (120)", number = "120"),
                EmergencyNumber(country = "China", category = "Fire (119)", number = "119"),

                // Australia
                EmergencyNumber(country = "Australia", category = "Emergency", number = "000")
            )
            dao.insertEmergencyNumbers(defaultNumbers)
        }
    }
}
