package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val identification: String,
    val email: String,
    val familyPhones: String, // comma-separated
    val country: String,
    val city: String
)

@Entity(tableName = "emergency_reports")
data class EmergencyReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alertType: String, // e.g., Terremoto, Robo, Asalto, Tsunami, Ascensor, Tapiado, etc.
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val severityColor: String, // YELLOW, ORANGE, RED, BLACK, GREEN
    val country: String,
    val city: String,
    val lastUpdate: Long,
    val isDisconnected: Boolean = false // If true, simulating device turned off/no signal
)

@Entity(tableName = "emergency_numbers")
data class EmergencyNumber(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val country: String,
    val category: String, // Policía, Bomberos, Ambulancia, Rescate, etc.
    val number: String,
    val canMessage: Boolean = true
)

@Dao
interface EmergencyDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // Emergency Reports
    @Query("SELECT * FROM emergency_reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<EmergencyReport>>

    @Query("SELECT * FROM emergency_reports WHERE country = :country AND city = :city ORDER BY timestamp DESC")
    fun getReportsByLocationFlow(country: String, city: String): Flow<List<EmergencyReport>>

    @Query("SELECT * FROM emergency_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Int): EmergencyReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: EmergencyReport): Long

    @Update
    suspend fun updateReport(report: EmergencyReport)

    @Query("DELETE FROM emergency_reports")
    suspend fun deleteAllReports()

    // Emergency Numbers
    @Query("SELECT * FROM emergency_numbers")
    fun getAllEmergencyNumbersFlow(): Flow<List<EmergencyNumber>>

    @Query("SELECT * FROM emergency_numbers WHERE country = :country")
    fun getEmergencyNumbersByCountryFlow(country: String): Flow<List<EmergencyNumber>>

    @Query("SELECT COUNT(*) FROM emergency_numbers")
    suspend fun getEmergencyNumbersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyNumbers(numbers: List<EmergencyNumber>)
}

@Database(entities = [UserProfile::class, EmergencyReport::class, EmergencyNumber::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emergencyDao(): EmergencyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safeme_sos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
