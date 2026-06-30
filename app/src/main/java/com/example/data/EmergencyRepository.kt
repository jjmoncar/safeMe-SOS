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
                EmergencyNumber(country = "USA", category = "Emergency (Police/Fire/Ambulance)", number = "911")
            )
            dao.insertEmergencyNumbers(defaultNumbers)
        }
    }
}
