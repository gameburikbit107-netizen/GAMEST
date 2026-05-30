package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val queryText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_wallet")
data class UserWallet(
    @PrimaryKey val id: Int = 1,
    val username: String = "GamerGanteng",
    val balance: Long = 500000L, // 500k IDR initial
    val isPremium: Boolean = false
)

@Entity(tableName = "donation_event")
data class DonationEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val streamerName: String,
    val donorName: String,
    val giftType: String,
    val amount: Long,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_channel")
data class StreamChannel(
    @PrimaryKey val id: String,
    val streamerName: String,
    val streamTitle: String,
    val gameName: String,
    val category: String,
    val viewerCount: Int,
    val likes: Int,
    val isLive: Boolean = true,
    val avatarColorHex: String = "#FF1493",
    val isCustomUserStream: Boolean = false
)

@Dao
interface MabarDao {
    // Search history
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getSearchHistory(): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistory)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchById(id: Int)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Wallet
    @Query("SELECT * FROM user_wallet WHERE id = 1")
    fun getWalletFlow(): Flow<UserWallet?>

    @Query("SELECT * FROM user_wallet WHERE id = 1")
    suspend fun getWalletDirect(): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWallet(wallet: UserWallet)

    // Donations
    @Query("SELECT * FROM donation_event ORDER BY timestamp DESC")
    fun getDonations(): Flow<List<DonationEvent>>

    @Query("SELECT * FROM donation_event WHERE streamerName = :streamerName ORDER BY timestamp DESC")
    fun getDonationsForStreamer(streamerName: String): Flow<List<DonationEvent>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDonation(donation: DonationEvent)

    // Stream Channels
    @Query("SELECT * FROM stream_channel ORDER BY viewerCount DESC")
    fun getAllStreams(): Flow<List<StreamChannel>>

    @Query("SELECT * FROM stream_channel WHERE isCustomUserStream = 0 ORDER BY viewerCount DESC")
    fun getPresetStreams(): Flow<List<StreamChannel>>

    @Query("SELECT COUNT(*) FROM stream_channel WHERE isCustomUserStream = 0")
    suspend fun getPresetStreamsCount(): Int

    @Query("SELECT * FROM stream_channel WHERE id = :id")
    suspend fun getStreamById(id: String): StreamChannel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: StreamChannel)

    @Delete
    suspend fun deleteStream(stream: StreamChannel)

    @Query("DELETE FROM stream_channel WHERE isCustomUserStream = 1")
    suspend fun deleteCustomUserStreams()
}

@Database(entities = [SearchHistory::class, UserWallet::class, DonationEvent::class, StreamChannel::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mabarDao(): MabarDao
}
