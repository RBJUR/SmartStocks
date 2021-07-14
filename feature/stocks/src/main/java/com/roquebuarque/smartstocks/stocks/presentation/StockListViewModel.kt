package com.roquebuarque.smartstocks.stocks.presentation

import com.jakewharton.rxrelay2.BehaviorRelay
import com.roquebuarque.smartstocks.StateViewModel
import com.roquebuarque.smartstocks.stocks.data.StockRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.Observable
import javax.inject.Inject

@HiltViewModel
class StockListViewModel @Inject constructor(
    private val repository: StockRepositoryImpl
) : StateViewModel<StockListEvent, StockListState>() {

    private val action = BehaviorRelay.createDefault<StockListEvent>(StockListEvent.Fetch)
    override val state: Observable<StockListState> =
        action
            .switchMap { event ->
                when (event) {
                    StockListEvent.Fetch -> fetchStocks().toObservable()
                }
            }

    private fun fetchStocks(): Flowable<StockListState> {
        return repository
            .getStockList()
            .scan(StockListState(),
                { state, result -> result.mapperToState(state) }
            )
    }

    override fun dispatch(event: StockListEvent) {
        action.accept(event)
    }
}