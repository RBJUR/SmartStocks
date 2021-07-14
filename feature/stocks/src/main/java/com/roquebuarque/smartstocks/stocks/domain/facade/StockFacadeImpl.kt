package com.roquebuarque.smartstocks.stocks.domain.facade

import android.util.Log
import com.google.gson.Gson
import com.roquebuarque.smartstocks.di.ComputationScheduler
import com.roquebuarque.smartstocks.network.Message
import com.roquebuarque.smartstocks.stocks.domain.provider.StockService
import com.roquebuarque.smartstocks.stocks.domain.models.ConnectionFailedException
import com.roquebuarque.smartstocks.stocks.domain.models.StockDto
import com.roquebuarque.smartstocks.stocks.domain.provider.StockLocal
import com.roquebuarque.smartstocks.stocks.domain.models.SupportedStocks
import com.roquebuarque.smartstocks.stocks.domain.provider.StockFacade
import com.tinder.scarlet.WebSocket.Event.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Flowable.*
import io.reactivex.Scheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockFacadeImpl @Inject constructor(
    private val remote: StockService,
    private val local: StockLocal,
) : StockFacade {

    private val gson by lazy { Gson() }
    override fun fetchAllStocks(): Flowable<List<StockDto>> {
        return whenConnected {
            local
                .retrieve()
                .toFlowable(BackpressureStrategy.LATEST)
        }
    }

    private fun <T> whenConnected(func: () -> Flowable<T>): Flowable<T> {
        return remote
            .observeWebSocketEvent()
            .flatMap {
                when (it) {
                    is OnConnectionOpened<*> -> subscribe()
                    is OnMessageReceived -> sendMessage(it.message as? com.tinder.scarlet.Message.Text)
                    is OnConnectionClosing -> throw ConnectionFailedException(it.shutdownReason.reason)
                    is OnConnectionClosed -> throw ConnectionFailedException(it.shutdownReason.reason)
                    is OnConnectionFailed -> throw  it.throwable
                }.run {
                    func()
                }
            }
    }

    private fun sendMessage(message: com.tinder.scarlet.Message.Text?) {
        message?.let {
            val stockDto = gson.fromJson(
                message.value,
                StockDto::class.java
            )
            local.save(stockDto)
        }

    }

    private fun subscribe() {
        SupportedStocks
            .values()
            .toList()
            .forEach { supportedStocks ->
                remote.sendSubscribe(Message(supportedStocks.isin))
            }
    }

}