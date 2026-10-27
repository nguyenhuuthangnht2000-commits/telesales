package com.nhakhoaquangninh.telesales

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.data.local.TokenManager

internal data class SimSubscriptionCandidate(
    val slotIndex: Int,
    val subscriptionId: Int,
    val iccId: String? = null,
    val phoneNumber: String? = null,
    val accountHandleIds: Set<String> = emptySet()
)

internal data class ResolvedPhoneSelection(
    val phoneNumber: String?,
    val source: String,
    val matchedSim: SimSubscriptionCandidate? = null
)

object OwnPhoneNumberResolver {
    private const val TAG = "OwnPhoneNumberResolver"

    /** Diagnostic only: does not change which number is used by resolve(). */
    fun logDiagnostics(
        context: Context,
        callId: String,
        workId: String,
        phoneAccountId: String? = null,
        viaNumber: String? = null
    ) {
        val appContext = context.applicationContext
        val account = TokenManager.getInstance(appContext).getSession()?.phoneNumber.normalize()
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED
        var state = if (granted) "available" else "permission_denied"
        var candidates = emptyList<SimSubscriptionCandidate>()
        val sims = if (!granted) {
            "unknown"
        } else {
            try {
                candidates = querySimCandidates(appContext)
                if (candidates.isEmpty()) {
                    state = "no_active_subscription"
                    "none"
                } else {
                    candidates.joinToString(";") { sub ->
                        "slot=${sub.slotIndex},subscription=${sub.subscriptionId},phone=${FileLogger.maskPhone(sub.phoneNumber)}"
                    }
                }
            } catch (_: SecurityException) {
                state = "permission_denied"
                "unknown"
            } catch (_: RuntimeException) {
                state = "query_failed"
                "unknown"
            }
        }
        val numbers = candidates.mapNotNull { it.phoneNumber.normalize() }
        val mismatch = if (state != "available" || account == null || numbers.isEmpty()) {
            "unknown"
        } else {
            numbers.none { canonicalPhone(it) == canonicalPhone(account) }.toString()
        }
        val selection = resolveSelection(
            sims = candidates,
            phoneAccountId = phoneAccountId,
            viaNumber = viaNumber,
            userNumber = { account }
        )
        val observedSource = if (state == "query_failed" && selection.phoneNumber == null) {
            "unknown"
        } else {
            selection.source
        }
        val callSubscription = when {
            selection.matchedSim != null ->
                "slot=${selection.matchedSim.slotIndex},subscription=${selection.matchedSim.subscriptionId}"
            !phoneAccountId.isNullOrBlank() ->
                "unmatched(${FileLogger.safeDiagnostic(phoneAccountId)})"
            else -> "unknown"
        }
        FileLogger.logLocal(
            appContext,
            "PHONE_SOURCE",
            "call_id=$callId work_id=$workId priority=sim_then_account observed_source=$observedSource " +
                "account_phone=${FileLogger.maskPhone(account)} " +
                "sim_state=$state sims=[$sims] account_matches_no_sim=$mismatch call_subscription=$callSubscription"
        )
    }

    fun resolve(
        context: Context,
        phoneAccountId: String? = null,
        viaNumber: String? = null
    ): String? {
        val appContext = context.applicationContext
        val sims = readSimCandidates(appContext)
        return resolveSelection(
            sims = sims,
            phoneAccountId = phoneAccountId,
            viaNumber = viaNumber,
            userNumber = { TokenManager.getInstance(appContext).getSession()?.phoneNumber }
        ).phoneNumber
    }

    internal fun selectPhoneNumber(simNumber: () -> String?, userNumber: () -> String?): String? =
        simNumber().normalize() ?: userNumber().normalize()

    internal fun resolveSelection(
        sims: List<SimSubscriptionCandidate>,
        phoneAccountId: String? = null,
        viaNumber: String? = null,
        userNumber: () -> String?
    ): ResolvedPhoneSelection {
        val matchedSim = matchSubscription(sims, phoneAccountId)
        val matchedSimPhone = matchedSim?.phoneNumber.normalize()
        if (matchedSimPhone != null) {
            return ResolvedPhoneSelection(
                phoneNumber = matchedSimPhone,
                source = "call_sim",
                matchedSim = matchedSim
            )
        }

        val normalizedVia = viaNumber.normalizeDialablePhone()
        if (normalizedVia != null) {
            return ResolvedPhoneSelection(
                phoneNumber = normalizedVia,
                source = "call_via_number",
                matchedSim = matchedSim
            )
        }

        val simsWithNumbers = sims.mapNotNull { sim ->
            sim.phoneNumber.normalize()?.let { sim to it }
        }
        if (simsWithNumbers.isNotEmpty()) {
            if (sims.size > 1) {
                val account = userNumber().normalize()
                if (account != null) {
                    val nonAccountSim = simsWithNumbers.firstOrNull { (_, phone) ->
                        canonicalPhone(phone) != canonicalPhone(account)
                    }
                    if (nonAccountSim != null) {
                        return ResolvedPhoneSelection(
                            phoneNumber = nonAccountSim.second,
                            source = "non_account_sim",
                            matchedSim = matchedSim
                        )
                    }
                }
            }
            val firstSim = simsWithNumbers.first()
            return ResolvedPhoneSelection(
                phoneNumber = firstSim.second,
                source = "sim",
                matchedSim = matchedSim
            )
        }

        val fallbackAccount = userNumber().normalize()
        return if (fallbackAccount != null) {
            ResolvedPhoneSelection(
                phoneNumber = fallbackAccount,
                source = "account_fallback",
                matchedSim = matchedSim
            )
        } else {
            ResolvedPhoneSelection(
                phoneNumber = null,
                source = "unavailable",
                matchedSim = matchedSim
            )
        }
    }

    internal fun matchSubscription(
        sims: List<SimSubscriptionCandidate>,
        phoneAccountId: String?
    ): SimSubscriptionCandidate? {
        val accountId = phoneAccountId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (sims.isEmpty()) return null

        sims.firstOrNull { sim ->
            sim.subscriptionId.toString() == accountId ||
                sim.accountHandleIds.any { it.equals(accountId, ignoreCase = true) } ||
                (!sim.iccId.isNullOrBlank() && sim.iccId.equals(accountId, ignoreCase = true))
        }?.let { return it }

        val normalizedAccountIcc = accountId.trimEnd('F', 'f')
        sims.firstOrNull { sim ->
            val handleMatches = sim.accountHandleIds.any { handleId ->
                val trimmedHandle = handleId.trim().trimEnd('F', 'f')
                trimmedHandle.equals(normalizedAccountIcc, ignoreCase = true)
            }
            val simIcc = sim.iccId?.trim()?.trimEnd('F', 'f')
            val iccMatches = !simIcc.isNullOrEmpty() && (
                simIcc.equals(normalizedAccountIcc, ignoreCase = true) ||
                    (simIcc.length >= 10 && normalizedAccountIcc.contains(simIcc, ignoreCase = true)) ||
                    (normalizedAccountIcc.length >= 10 && simIcc.contains(normalizedAccountIcc, ignoreCase = true))
                )
            handleMatches || iccMatches
        }?.let { return it }

        val trailingToken = accountId
            .substringAfterLast('/')
            .substringAfterLast(':')
            .substringAfterLast(';')
            .trim()
        if (trailingToken.isNotEmpty() && trailingToken != accountId) {
            sims.firstOrNull { sim ->
                sim.subscriptionId.toString() == trailingToken ||
                    sim.accountHandleIds.any { it.equals(trailingToken, ignoreCase = true) }
            }?.let { return it }
        }

        return null
    }

    private fun readSimCandidates(appContext: Context): List<SimSubscriptionCandidate> {
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        return try {
            querySimCandidates(appContext)
        } catch (_: SecurityException) {
            Log.w(TAG, "Không thể đọc số điện thoại của SIM")
            emptyList()
        } catch (_: RuntimeException) {
            Log.w(TAG, "Thiết bị không cung cấp số điện thoại của SIM")
            emptyList()
        }
    }

    @Suppress("MissingPermission")
    private fun querySimCandidates(appContext: Context): List<SimSubscriptionCandidate> {
        val manager = appContext.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        val telephonyManager = appContext.getSystemService(TelephonyManager::class.java)
        val telecomManager = appContext.getSystemService(TelecomManager::class.java)
        val hasPhoneState = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val handleIdsBySubId = mutableMapOf<Int, MutableSet<String>>()
        val telecomPhoneBySubId = mutableMapOf<Int, String>()
        if (hasPhoneState && telecomManager != null && telephonyManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val handles = runCatching { telecomManager.callCapablePhoneAccounts.orEmpty() }.getOrDefault(emptyList())
            for (handle in handles) {
                val subId = runCatching { telephonyManager.getSubscriptionId(handle) }.getOrNull() ?: continue
                if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) continue
                val handleId = handle.id?.trim()?.takeIf { it.isNotEmpty() }
                if (handleId != null) {
                    handleIdsBySubId.getOrPut(subId) { linkedSetOf() }.add(handleId)
                }
                val account = runCatching { telecomManager.getPhoneAccount(handle) }.getOrNull()
                val addressPhone = listOfNotNull(account?.subscriptionAddress, account?.address)
                    .firstOrNull { it.scheme.equals("tel", ignoreCase = true) }
                    ?.schemeSpecificPart
                    .normalizeDialablePhone()
                if (addressPhone != null && !telecomPhoneBySubId.containsKey(subId)) {
                    telecomPhoneBySubId[subId] = addressPhone
                }
            }
        }

        return manager.activeSubscriptionInfoList.orEmpty().map { subscription ->
            val subId = subscription.subscriptionId
            val numberFromManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { manager.getPhoneNumber(subId) }.getOrNull().normalize()
            } else {
                null
            }
            @Suppress("DEPRECATION")
            val numberFromInfo = runCatching { subscription.number }.getOrNull().normalize()
            @Suppress("DEPRECATION")
            val numberFromTelephony = runCatching {
                telephonyManager?.createForSubscriptionId(subId)?.line1Number
            }.getOrNull().normalize()
            val number = numberFromManager
                ?: numberFromInfo
                ?: numberFromTelephony
                ?: telecomPhoneBySubId[subId]
            val iccId = runCatching { subscription.iccId }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            SimSubscriptionCandidate(
                slotIndex = subscription.simSlotIndex,
                subscriptionId = subId,
                iccId = iccId,
                phoneNumber = number,
                accountHandleIds = handleIdsBySubId[subId].orEmpty()
            )
        }
    }

    internal fun canonicalPhone(value: String): String = value.filter(Char::isDigit).let {
        if (it.startsWith("84") && it.length == 11) "0" + it.drop(2) else it
    }

    private fun String?.normalize(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun String?.normalizeDialablePhone(): String? {
        val trimmed = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val digits = trimmed.filter(Char::isDigit)
        return if (digits.length in 9..13) trimmed else null
    }
}
