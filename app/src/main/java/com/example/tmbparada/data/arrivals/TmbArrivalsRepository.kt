package com.example.tmbparada.data.arrivals

import com.example.tmbparada.domain.ArrivalsRepository
import com.example.tmbparada.domain.Clock
import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.RefreshError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException

class TmbArrivalsRepository(
    private val apiClient: TmbApiClient,
    private val credentials: TmbCredentials,
    private val clock: Clock = Clock.System,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ArrivalsRepository {

    override suspend fun fetchArrivals(stopCode: String): ArrivalsResult {
        if (!credentials.isConfigured) return ArrivalsResult.Failure(RefreshError.MISSING_CREDENTIALS)

        return withContext(ioDispatcher) {
            try {
                val body = apiClient.fetchStopArrivals(stopCode)
                val receivedAt = clock.nowMillis()
                val arrivals = TmbResponseParser.parse(body).map { parsed ->
                    Arrival(
                        line = parsed.line,
                        destination = parsed.destination,
                        arrivalAtMillis = receivedAt + parsed.secondsUntilArrival * 1_000L,
                    )
                }
                ArrivalsResult.Success(arrivals)
            } catch (error: TmbHttpException) {
                ArrivalsResult.Failure(
                    if (error.code == 401 || error.code == 403) RefreshError.UNAUTHORIZED else RefreshError.SERVER,
                )
            } catch (error: IOException) {
                ArrivalsResult.Failure(RefreshError.NETWORK)
            } catch (error: SerializationException) {
                ArrivalsResult.Failure(RefreshError.INVALID_RESPONSE)
            }
        }
    }
}
