package com.mycelium.wallet.activity.modern.model.accounts

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.mycelium.bequant.*
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_TITLE_TYPE
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.activity.util.getBTCSingleAddressAccounts
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wallet.exchange.ValueSum
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.bch.bip44.getBCHBip44Accounts
import com.mycelium.wapi.wallet.bch.single.getBCHSingleAddressAccounts
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.colu.getColuAccounts
import com.mycelium.wapi.wallet.erc20.getERC20Accounts
import com.mycelium.wapi.wallet.eth.getEthAccounts
import com.squareup.otto.Subscribe
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class is intended to monitor current accounts and must post changes as soon as accounts list was updated.
 * @see AccountsViewLiveData.getValue retuns list of current accounts, with filtered hidden ones.
 */
class AccountsViewLiveData(private val mbwManager: MbwManager) : LiveData<List<AccountsGroupModel>>() {
    // List of all currently available accounts
    private var accountsList = Collections.emptyList<AccountsGroupModel>()
    private val executionService: ExecutorService

    init {
        value = Collections.emptyList()
        executionService = Executors.newCachedThreadPool()
        updateData()
    }

    override fun onActive() {
        MbwManager.getEventBus().register(this)
    }

    override fun onInactive() {
        MbwManager.getEventBus().unregister(this)
    }

    @Subscribe
    fun onAccountsListChanged(event: AccountListChanged) {
        updateData()
    }

    /**
     * Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
     */
    @SuppressLint("StaticFieldLeak")
    private inner class DataUpdateAsyncTask : AsyncTask<Void, List<AccountsGroupModel>, List<AccountsGroupModel>>() {
        override fun doInBackground(vararg voids: Void): List<AccountsGroupModel> {
            val walletManager = mbwManager.getWalletManager(false)
            val accountsList: MutableList<AccountsGroupModel> = mutableListOf()

            mutableListOf(R.string.active_hd_accounts_name to walletManager.getBTCBip44Accounts(),
                    R.string.active_bitcoin_sa_group_name to walletManager.getBTCSingleAddressAccounts(),
                    R.string.bitcoin_cash_hd to walletManager.getBCHBip44Accounts(),
                    R.string.bitcoin_cash_sa to walletManager.getBCHSingleAddressAccounts(),
                    R.string.digital_assets to getColuAccounts(walletManager),
                    R.string.eth_accounts_name to getEthERC20Accounts(walletManager)
            ).apply {
                if ((BequantPreference.isLogged() && SettingsPreference.isEnabled(Constants.PARTNER_ID)) ||
                        (!BequantPreference.isLogged() && SettingsPreference.isContentEnabled(Constants.PARTNER_ID))) {
                    this.add(R.string.bequant_trading_account to getInvestmentAccounts(walletManager))
                }
            }.forEach {
                val accounts = walletManager.getActiveAccountsFrom(sortAccounts(it.second))
                if (accounts.isNotEmpty()) {
                    val sum = getSpendableBalance(accounts)
                    accountsList.add(AccountsGroupModel(it.first, GROUP_TITLE_TYPE, sum, accountsToViewModel(accounts),
                            accounts[0].basedOnCoinType, it.second.first() is InvestmentAccount))
                }
            }
            if (value!!.isEmpty()) {
                publishProgress(accountsList)
            }

            val archivedList = walletManager.getArchivedAccounts()
            if (archivedList.isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.archive_name, GROUP_ARCHIVED_TITLE_TYPE, null,
                        accountsToViewModel(archivedList), archivedList[0].basedOnCoinType, false))
            }
            if (accountsList == value) {
                cancel(true)
            }
            return accountsList
        }

        private fun getColuAccounts(walletManager: WalletManager): List<WalletAccount<out GenericAddress>> =
                walletManager.getColuAccounts() + walletManager.getColuAccounts().mapNotNull { Utils.getLinkedAccount(it, walletManager.getAccounts()) }

        private fun getEthERC20Accounts(walletManager: WalletManager): List<WalletAccount<out GenericAddress>> =
                walletManager.getEthAccounts() + walletManager.getERC20Accounts()

        private fun getInvestmentAccounts(walletManager: WalletManager): List<WalletAccount<out GenericAddress>> =
                walletManager.getInvestmentAccounts()

        private fun accountsToViewModel(accounts: Collection<WalletAccount<out GenericAddress>>) =
                accounts.map {
                    if (it is InvestmentAccount) {
                        BQExchangeRateManager.requestOptionalRefresh()
                        AccountInvestmentViewModel(it, it.accountBalance.confirmed.toStringWithUnit())
                    } else AccountViewModel(it, mbwManager)
                }

        private fun sortAccounts(accounts: Collection<WalletAccount<out GenericAddress>>) =
                Utils.sortAccounts(accounts, mbwManager.metadataStorage)

        @SafeVarargs
        override fun onProgressUpdate(vararg values: List<AccountsGroupModel>) {
            super.onProgressUpdate(*values)
            accountsList = values[0]
            updateList()
        }

        override fun onPostExecute(result: List<AccountsGroupModel>) {
            accountsList = result
            updateList()
        }
    }

    private fun updateData() {
        DataUpdateAsyncTask().executeOnExecutor(executionService)
    }

    /**
     * This method is mainly intended to be able to quickly collapse groups.
     */
    private fun updateList() {
        if (value != accountsList) {
            value = accountsList
        }
    }

    companion object {
        private fun getSpendableBalance(walletAccountList: List<WalletAccount<out GenericAddress>>): ValueSum {
            val sum = ValueSum()
            for (account in walletAccountList) {
                if (account.isActive) {
                    sum.add(account.accountBalance.spendable)
                }
            }
            return sum
        }
    }
}