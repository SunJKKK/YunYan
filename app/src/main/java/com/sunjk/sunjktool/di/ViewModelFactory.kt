package com.sunjk.sunjktool.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.feature.countdown.edit.CountdownEditViewModel
import com.sunjk.sunjktool.feature.countdown.list.CountdownListViewModel
import com.sunjk.sunjktool.feature.home.HomeViewModel
import com.sunjk.sunjktool.feature.learninglog.detail.LogDetailViewModel
import com.sunjk.sunjktool.feature.learninglog.edit.LogEditViewModel

class HomeVMF(private val repo: LogRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repo) as T
}

class LogEditVMF(
    private val repo: LogRepository,
    private val logId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LogEditViewModel(repo, logId) as T
}

class LogDetailVMF(
    private val repo: LogRepository,
    private val logId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LogDetailViewModel(repo, logId) as T
}

class CountdownListVMF(private val repo: CountdownRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CountdownListViewModel(repo) as T
}

class CountdownEditVMF(
    private val repo: CountdownRepository,
    private val countdownId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CountdownEditViewModel(repo, countdownId) as T
}
