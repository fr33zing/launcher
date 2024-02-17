package dev.fr33zing.launcher

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.fr33zing.launcher.data.persistent.AppDatabase
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

typealias NextAlarmFlow = Flow<AlarmClockInfo?>

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, "database").build()

    @Singleton
    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Singleton
    @Provides
    fun provideNextAlarmFlow(@ApplicationContext context: Context): NextAlarmFlow = channelFlow {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intentFilter = IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    trySend(alarmManager.nextAlarmClock)
                }
            }
        context.registerReceiver(receiver, intentFilter)
        send(alarmManager.nextAlarmClock)
    }
}
